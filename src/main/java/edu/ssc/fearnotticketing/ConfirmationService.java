package edu.ssc.fearnotticketing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfirmationService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
        task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        }
    );

    public void sendConfirmation(String seatId) {
        executor.submit(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            System.out.println(
                "[ASYNC] Reservation confirmation processed for seat "
                + seatId
            );
        });
    }
}