package edu.ssc.fearnotticketing;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class JsonReservationRepository implements ReservationRepository {
    private final File file;
    private final Gson gson;

    public JsonReservationRepository(String fileName) {
        this.file = new File(fileName);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public List<Reservation> loadReservations() {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            List<Reservation> reservations = gson.fromJson(
                reader,
                new TypeToken<List<Reservation>>() {}.getType());

            if (reservations == null) {
                return new ArrayList<>();
            }

            return reservations;
        } catch (IOException e) {
            throw new RuntimeException("Could not load reservations.", e);
        }
    }

    @Override
    public void saveReservations(List<Reservation> reservations) {
        File parent = file.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(reservations, writer);
        } catch (IOException e) {
            throw new RuntimeException("Could not save reservations.", e);
        }
    }
}