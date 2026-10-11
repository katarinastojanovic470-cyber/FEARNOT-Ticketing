package edu.ssc.fearnotticketing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TcpReservationClient {
    public static void main(String[] args) {
        try (
            Socket socket = new Socket("localhost", 5050);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );
            PrintWriter writer = new PrintWriter(
                socket.getOutputStream(),
                true,
                StandardCharsets.UTF_8
            );
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Connected to FEARNOT Ticketing.");
            System.out.println("Enter a command, or type QUIT to exit.");
            System.out.println();
            System.out.println("Commands:");
            System.out.println("LIST");
            System.out.println("RESERVE|seatId|customerName");
            System.out.println("GET|reservationId|accessToken");
            System.out.println("CANCEL|reservationId|accessToken");
            System.out.println();

            while (true) {
                System.out.print("> ");
                String command = scanner.nextLine();

                writer.println(command);

                String response = reader.readLine();

                if (response == null) {
                    System.out.println("Server disconnected.");
                    break;
                }

                System.out.println(response);

                if (command.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(
                "Could not connect. Make sure the TCP server is running."
            );
        }
    }
}