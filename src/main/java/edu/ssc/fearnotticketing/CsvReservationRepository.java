package edu.ssc.fearnotticketing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvReservationRepository implements ReservationRepository {
    private final Path file;

    public CsvReservationRepository(String fileName) {
        this.file = Path.of(fileName);
    }

    @Override
    public List<Reservation> loadReservations() {
        List<Reservation> reservations = new ArrayList<>();

        if (!Files.exists(file)) {
            return reservations;
        }

        try {
            List<String> lines = Files.readAllLines(file);

            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(",", -1);

                if (values.length != 4) {
                    throw new IOException("Invalid reservation data in CSV file.");
                }

                Reservation reservation = new Reservation(
                    values[0],
                    values[1],
                    values[2],
                    values[3]
                );

                reservations.add(reservation);
            }

            return reservations;
        } catch (IOException e) {
            throw new RuntimeException("Could not load reservations.", e);
        }
    }

    @Override
    public void saveReservations(List<Reservation> reservations) {
        File parent = file.toFile().getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        List<String> lines = new ArrayList<>();
        lines.add("reservationId,seatId,customerName,accessToken");

        for (Reservation reservation : reservations) {
            lines.add(
                reservation.getReservationId() + ","
                + reservation.getSeatId() + ","
                + reservation.getCustomerName() + ","
                + reservation.getAccessToken()
            );
        }

        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new RuntimeException("Could not save reservations.", e);
        }
    }
}