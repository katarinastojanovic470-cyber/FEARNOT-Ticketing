package edu.ssc.fearnotticketing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReservationServiceTest {
    private ReservationService service;

    @BeforeEach
    public void setUp() {
        ReservationRepository repository = new ReservationRepository() {
            private List<Reservation> savedReservations = new ArrayList<>();

            @Override
            public List<Reservation> loadReservations() {
                return new ArrayList<>(savedReservations);
            }

            @Override
            public void saveReservations(List<Reservation> reservations) {
                savedReservations = new ArrayList<>(reservations);
            }
        };

        service = new ReservationService(repository);
    }

    @Test
    public void testInitialSeatAvailability() {
        assertEquals(6, service.getAvailableSeats().size());
    }

    @Test
    public void testSuccessfulReservation() {
        Reservation reservation = service.makeReservation("VIP-A1", "Alex");

        assertNotNull(reservation.getReservationId());
        assertNotNull(reservation.getAccessToken());
        assertEquals("VIP-A1", reservation.getSeatId());
        assertFalse(service.getAvailableSeats().contains("VIP-A1"));
    }

    @Test
    public void testCannotReserveSameSeatTwice() {
        service.makeReservation("VIP-A1", "Alex");

        assertThrows(
            IllegalStateException.class,
            () -> service.makeReservation("VIP-A1", "Jordan")
        );
    }

    @Test
    public void testInvalidSeatIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.makeReservation("INVALID-SEAT", "Alex")
        );
    }

    @Test
    public void testCancellationMakesSeatAvailableAgain() {
        Reservation reservation = service.makeReservation("VIP-A1", "Alex");

        boolean cancelled = service.cancelReservation(
            reservation.getReservationId(),
            reservation.getAccessToken()
        );

        assertTrue(cancelled);
        assertTrue(service.getAvailableSeats().contains("VIP-A1"));
    }

    @Test
    public void testWrongAccessTokenCannotCancelReservation() {
        Reservation reservation = service.makeReservation("VIP-A1", "Alex");

        boolean cancelled = service.cancelReservation(
            reservation.getReservationId(),
            "wrong-token"
        );

        assertFalse(cancelled);
        assertFalse(service.getAvailableSeats().contains("VIP-A1"));
    }
    
    @Test
    public void testConcurrentReservationsOnlyOneSucceeds() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = pool.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    service.makeReservation("VIP-A1", "Alex");
                    return true;
                } catch (IllegalStateException e) {
                    return false;
                }
            });

            Future<Boolean> second = pool.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    service.makeReservation("VIP-A1", "Jordan");
                    return true;
                } catch (IllegalStateException e) {
                    return false;
                }
            });

            ready.await();
            start.countDown();

            boolean firstSucceeded = first.get();
            boolean secondSucceeded = second.get();

            assertTrue(firstSucceeded != secondSucceeded);
            assertEquals(5, service.getAvailableSeats().size());
        } finally {
            pool.shutdownNow();
        }
    }
}