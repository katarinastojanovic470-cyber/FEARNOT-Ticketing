package edu.ssc.fearnotticketing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpReservationServer implements AutoCloseable {
    private final ReservationService service;
    private final int port;
    private final AtomicInteger connectionCounter = new AtomicInteger();

    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private volatile boolean running;

    public TcpReservationServer(ReservationService service, int port) {
        this.service = service;
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        clientPool = Executors.newCachedThreadPool();
        running = true;

        Thread acceptThread = new Thread(() -> {
            System.out.println("----------------------------------------------");
            System.out.println("             FEARNOT Ticketing");
            System.out.println("                TCP Server");
            System.out.println("----------------------------------------------");
            System.out.println("Server started on port " + port);
            System.out.println("Waiting for client connections...");
            System.out.println();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientNumber = connectionCounter.incrementAndGet();

                    clientPool.submit(
                        () -> handleClient(clientSocket, clientNumber)
                    );
                } catch (IOException e) {
                    if (running) {
                        System.out.println("Could not accept a client connection.");
                    }
                }
            }
        });

        acceptThread.start();
    }

    private void handleClient(Socket socket, int clientNumber) {
        
    	System.out.println("Client #" + clientNumber + " connected.");

        try (
            Socket client = socket;
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    client.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );
            PrintWriter writer = new PrintWriter(
                client.getOutputStream(),
                true,
                StandardCharsets.UTF_8
            )
        ) {
            String request;

            while ((request = reader.readLine()) != null) {
                String command = request.split("\\|", -1)[0].toUpperCase();

                if (command.equals("RESERVE")) {
                    String[] parts = request.split("\\|", -1);

                    if (parts.length > 1) {
                        System.out.println(
                            "Client #" + clientNumber
                            + " request received: RESERVE seat=" + parts[1]
                        );
                    } else {
                        System.out.println(
                            "Client #" + clientNumber
                            + " request received: RESERVE"
                        );
                    }
                } else {
                    System.out.println(
                        "Client #" + clientNumber
                        + " request received: " + command
                    );
                }

                String response = processRequest(request);
                writer.println(response);

                String[] responseParts = response.split("\\|", -1);
                System.out.println(
                    "Response sent to client #" + clientNumber + ": "
                    + responseParts[0]
                    + (responseParts.length > 1
                        ? "|" + responseParts[1] : "")
                );

                if (command.equals("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(
                "Communication ended with client #" + clientNumber + "."
            );
        } finally {
            System.out.println("Client #" + clientNumber + " disconnected.");
            System.out.println("Waiting for client connections...");
            System.out.println();
        }
    }

    private String processRequest(String request) {
        String[] parts = request.split("\\|", -1);

        try {
            if (parts[0].equalsIgnoreCase("LIST") && parts.length == 1) {
                List<String> seats = service.getAvailableSeats();
                return "OK|AVAILABLE|" + String.join(",", seats);
            }

            if (parts[0].equalsIgnoreCase("RESERVE") && parts.length == 3) {
                Reservation reservation = service.makeReservation(
                    parts[1],
                    parts[2]
                );

                return "OK|RESERVED|"
                    + reservation.getReservationId() + "|"
                    + reservation.getSeatId() + "|"
                    + reservation.getAccessToken();
            }

            if (parts[0].equalsIgnoreCase("GET") && parts.length == 3) {
                Reservation reservation = service.getReservation(
                    parts[1],
                    parts[2]
                );

                if (reservation == null) {
                    return "ERROR|NOT_FOUND|Reservation not found.";
                }

                return "OK|RESERVATION|"
                    + reservation.getReservationId() + "|"
                    + reservation.getSeatId() + "|"
                    + reservation.getCustomerName();
            }

            if (parts[0].equalsIgnoreCase("CANCEL") && parts.length == 3) {
                boolean cancelled = service.cancelReservation(
                    parts[1],
                    parts[2]
                );

                if (cancelled) {
                    return "OK|CANCELLED";
                }

                return "ERROR|NOT_FOUND|Reservation not found.";
            }

            if (parts[0].equalsIgnoreCase("QUIT") && parts.length == 1) {
                return "OK|BYE";
            }

            return "ERROR|INVALID_REQUEST|Invalid command or arguments.";
        } catch (IllegalArgumentException e) {
            return "ERROR|INVALID_REQUEST|Invalid reservation details.";
        } catch (IllegalStateException e) {
            String message = e.getMessage();

            if (message != null && message.contains("already reserved")) {
                return "ERROR|SEAT_UNAVAILABLE|That seat is already reserved.";
            }

            return "ERROR|OPERATION_FAILED|The operation could not be completed.";
        } catch (RuntimeException e) {
            return "ERROR|OPERATION_FAILED|The operation could not be completed.";
        }
    }

    @Override
    public void close() throws IOException {
        running = false;

        if (serverSocket != null) {
            serverSocket.close();
        }

        if (clientPool != null) {
            clientPool.shutdownNow();
        }
    }
}