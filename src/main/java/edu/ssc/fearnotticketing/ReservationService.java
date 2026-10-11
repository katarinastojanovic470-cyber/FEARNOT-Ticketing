package edu.ssc.fearnotticketing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReservationService {
    private final ReservationRepository repository;
    private final List<String> seats;
    private final List<Reservation> reservations;
    private final ConfirmationService confirmationService =
    	    new ConfirmationService();

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;

        this.seats = Arrays.asList(
            "VIP-A1",
            "VIP-A2",
            "FLOOR-B1",
            "FLOOR-B2",
            "UPPER-C1",
            "UPPER-C2"
        );

        this.reservations = new ArrayList<>(repository.loadReservations());

        validateLoadedReservations();
    }

    private void validateLoadedReservations() {
        Set<String> reservedSeats = new HashSet<>();
        Set<String> reservationIds = new HashSet<>();

        for (Reservation reservation : reservations) {
            if (reservation == null
                || reservation.getReservationId() == null
                || reservation.getSeatId() == null
                || reservation.getCustomerName() == null
                || reservation.getAccessToken() == null
                || !seats.contains(reservation.getSeatId())
                || !reservedSeats.add(reservation.getSeatId())
                || !reservationIds.add(reservation.getReservationId())) {
                throw new IllegalStateException("Saved reservation data is invalid.");
            }
        }
    }

    public synchronized List<String> getAvailableSeats() {
        List<String> availableSeats = new ArrayList<>(seats);

        for (Reservation reservation : reservations) {
            availableSeats.remove(reservation.getSeatId());
        }

        return availableSeats;
    }

    public synchronized Reservation makeReservation(
        String seatId,
        String customerName
    ) {
    	if (seatId == null) {
    	    throw new IllegalArgumentException("Invalid seat ID.");
    	}

    	seatId = seatId.trim().toUpperCase();

    	if (!seats.contains(seatId)) {
    	    throw new IllegalArgumentException("Invalid seat ID.");
    	}

        if (customerName == null
            || customerName.trim().isEmpty()
            || customerName.length() > 60
            || customerName.contains(",")
            || customerName.contains("\n")
            || customerName.contains("\r")) {
            throw new IllegalArgumentException("Invalid customer name.");
        }

        if (!getAvailableSeats().contains(seatId)) {
            throw new IllegalStateException("That seat is already reserved.");
        }

        Reservation reservation = new Reservation(
            UUID.randomUUID().toString(),
            seatId,
            customerName.trim(),
            UUID.randomUUID().toString()
        );

        reservations.add(reservation);

        try {
            repository.saveReservations(new ArrayList<>(reservations));
        } catch (RuntimeException e) {
            reservations.remove(reservation);
            throw new IllegalStateException("Could not save the reservation.", e);
        }

        confirmationService.sendConfirmation(reservation.getSeatId());
        return reservation;
    }

    public synchronized Reservation getReservation(
        String reservationId,
        String accessToken
    ) {
        if (reservationId == null || accessToken == null) {
            return null;
        }

        for (Reservation reservation : reservations) {
            if (reservation.getReservationId().equals(reservationId)
                && reservation.getAccessToken().equals(accessToken)) {
                return reservation;
            }
        }

        return null;
    }

    public synchronized boolean cancelReservation(
        String reservationId,
        String accessToken
    ) {
        Reservation reservation = getReservation(reservationId, accessToken);

        if (reservation == null) {
            return false;
        }

        reservations.remove(reservation);

        try {
            repository.saveReservations(new ArrayList<>(reservations));
        } catch (RuntimeException e) {
            reservations.add(reservation);
            throw new IllegalStateException("Could not save the cancellation.", e);
        }

        return true;
    }
}