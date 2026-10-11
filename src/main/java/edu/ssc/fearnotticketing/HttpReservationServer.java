package edu.ssc.fearnotticketing;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class HttpReservationServer {
    private final ReservationService service;
    private final Gson gson = new Gson();
    private HttpServer server;

    public HttpReservationServer(ReservationService service, int port)
            throws IOException {
        this.service = service;
        server = HttpServer.create(new InetSocketAddress(port), 0);
    }

    public void start() {
        server.createContext("/api/seats", this::handleSeats);
        server.createContext("/api/reservations", this::handleReservations);
        server.setExecutor(
            java.util.concurrent.Executors.newCachedThreadPool()
        );
        server.start();

        System.out.println("HTTP API started on port 8080");
    }

    private void handleSeats(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                sendResponse(exchange, 405,
                    Map.of("error", "Method not allowed."));
                return;
            }

            List<String> seats = service.getAvailableSeats();

            sendResponse(exchange, 200,
                Map.of("availableSeats", seats, "count", seats.size()));
        } finally {
            exchange.close();
        }
    }

    private void handleReservations(HttpExchange exchange)
            throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String basePath = "/api/reservations";

            if (method.equals("POST")) {
                if (!path.equals(basePath)) {
                    sendResponse(exchange, 404,
                        Map.of("error", "Endpoint not found."));
                    return;
                }

                ReservationRequest request;

                try (InputStream input = exchange.getRequestBody()) {
                    byte[] body = input.readNBytes(4097);

                    if (body.length == 0 || body.length > 4096) {
                        sendResponse(exchange, 400,
                            Map.of("error", "Invalid request body."));
                        return;
                    }

                    request = gson.fromJson(
                        new String(body, StandardCharsets.UTF_8),
                        ReservationRequest.class
                    );
                } catch (JsonSyntaxException | IllegalStateException e) {
                    sendResponse(exchange, 400,
                        Map.of("error", "Invalid JSON request."));
                    return;
                }

                if (request == null
                    || request.seatId == null
                    || request.customerName == null) {
                    sendResponse(exchange, 400,
                        Map.of("error", "seatId and customerName are required."));
                    return;
                }

                Reservation reservation;

                try {
                    reservation = service.makeReservation(
                        request.seatId, request.customerName
                    );
                } catch (IllegalArgumentException e) {
                    sendResponse(exchange, 400,
                        Map.of("error", "Invalid reservation details."));
                    return;
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null
                        && e.getMessage().contains("already reserved")) {
                        sendResponse(exchange, 409,
                            Map.of("error", "That seat is already reserved."));
                    } else {
                        sendResponse(exchange, 500,
                            Map.of("error", "Reservation could not be saved."));
                    }
                    return;
                }

                sendResponse(exchange, 201,
                    Map.of(
                        "message", "Reservation created.",
                        "reservationId", reservation.getReservationId(),
                        "seatId", reservation.getSeatId(),
                        "customerName", reservation.getCustomerName(),
                        "accessToken", reservation.getAccessToken()
                    ));
                return;
            }

            if (method.equals("GET") || method.equals("DELETE")) {
                String prefix = basePath + "/";

                if (!path.startsWith(prefix)
                    || path.length() == prefix.length()) {
                    sendResponse(exchange, 404,
                        Map.of("error", "Reservation endpoint not found."));
                    return;
                }

                String reservationId = path.substring(prefix.length());

                if (reservationId.contains("/")) {
                    sendResponse(exchange, 404,
                        Map.of("error", "Reservation endpoint not found."));
                    return;
                }

                String token = getQueryParameter(
                    exchange.getRequestURI().getRawQuery(), "token"
                );

                if (token == null || token.isEmpty()) {
                    sendResponse(exchange, 400,
                        Map.of("error", "A reservation token is required."));
                    return;
                }

                if (method.equals("GET")) {
                    Reservation reservation = service.getReservation(
                        reservationId, token
                    );

                    if (reservation == null) {
                        sendResponse(exchange, 404,
                            Map.of("error", "Reservation not found."));
                        return;
                    }

                    sendResponse(exchange, 200,
                        Map.of(
                            "reservationId", reservation.getReservationId(),
                            "seatId", reservation.getSeatId(),
                            "customerName", reservation.getCustomerName()
                        ));
                    return;
                }

                boolean cancelled = service.cancelReservation(
                    reservationId, token
                );

                if (!cancelled) {
                    sendResponse(exchange, 404,
                        Map.of("error", "Reservation not found."));
                    return;
                }

                sendResponse(exchange, 200,
                    Map.of("message", "Reservation cancelled."));
                return;
            }

            exchange.getResponseHeaders().set(
                "Allow", "GET, POST, DELETE"
            );
            sendResponse(exchange, 405,
                Map.of("error", "Method not allowed."));
        } finally {
            exchange.close();
        }
    }

    private String getQueryParameter(String query, String name) {
        if (query == null) {
            return null;
        }

        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            String key = URLDecoder.decode(
                parts[0], StandardCharsets.UTF_8
            );

            if (key.equals(name) && parts.length == 2) {
                return URLDecoder.decode(
                    parts[1], StandardCharsets.UTF_8
                );
            }
        }

        return null;
    }

    private void sendResponse(
            HttpExchange exchange, int status, Object response)
            throws IOException {
        byte[] body = gson.toJson(response)
            .getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, body.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static class ReservationRequest {
        private String seatId;
        private String customerName;
    }
}