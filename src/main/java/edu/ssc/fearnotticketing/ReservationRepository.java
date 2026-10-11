package edu.ssc.fearnotticketing;

import java.util.List;

public interface ReservationRepository {
    List<Reservation> loadReservations();

    void saveReservations(List<Reservation> reservations);
}