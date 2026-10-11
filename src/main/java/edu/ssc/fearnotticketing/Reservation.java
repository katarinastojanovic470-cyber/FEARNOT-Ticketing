package edu.ssc.fearnotticketing;

public class Reservation {
    private String reservationId;
    private String seatId;
    private String customerName;
    private String accessToken;
    

    public Reservation() {
    }

    public Reservation(String reservationId, String seatId, String customerName, String accessToken) {
        this.reservationId = reservationId;
        this.seatId = seatId;
        this.customerName = customerName;
        this.accessToken = accessToken;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getSeatId() {
        return seatId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
