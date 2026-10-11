package edu.ssc.fearnotticketing;

public class App {
    public static void main(String[] args) {
        String storage = System.getProperty("reservation.storage", "json");

        ReservationRepository repository;

        if (storage.equalsIgnoreCase("csv")) {
            repository = new CsvReservationRepository(
                "data/reservations.csv"
            );
        } else if (storage.equalsIgnoreCase("json")) {
            repository = new JsonReservationRepository(
                "data/reservations.json"
            );
        } else {
            System.out.println(
                "Invalid storage option. Use json or csv."
            );
            return;
        }

        ReservationService service = new ReservationService(repository);
        TcpReservationServer server = new TcpReservationServer(service, 5050);

        try {
            HttpReservationServer httpServer =
                new HttpReservationServer(service, 8080);

            server.start();
            httpServer.start();

            System.out.println("FEARNOT Ticketing is running.");
        } catch (Exception e) {
            System.out.println("Could not start the servers.");
            e.printStackTrace();
        }
    }
}