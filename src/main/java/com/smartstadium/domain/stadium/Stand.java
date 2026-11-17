package com.smartstadium.domain.stadium;

import com.smartstadium.domain.seat.Seat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Stand {
    private final String id;
    private final String name;
    private final List<Seat> seats = new ArrayList<>();

    public Stand(String id, String name) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stand id and name are required.");
        }
        this.id = id.trim();
        this.name = name.trim();
    }

    public void addSeat(Seat seat) {
        Objects.requireNonNull(seat, "Seat cannot be null");
        boolean duplicate = seats.stream().anyMatch(existing ->
                existing.getId().equals(seat.getId()) ||
                existing.getLabel().equalsIgnoreCase(seat.getLabel()));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate seat id or label in stand " + name);
        }
        seats.add(seat);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public long getAvailableSeatCount() {
        return seats.stream().filter(Seat::isAvailable).count();
    }
}
