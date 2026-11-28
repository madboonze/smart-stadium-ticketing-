package com.smartstadium.domain.stadium;

import com.smartstadium.domain.seat.Seat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A single row of seats inside a {@link Section}.
 *
 * <p>A row is the third level of the seating hierarchy:
 * {@code Stadium -> Stand -> Section -> SeatRow -> Seat}. It owns its seats and
 * guards against duplicate seat identities, mirroring the protection that
 * {@link Stand} already applies to the seats registered directly on it.</p>
 *
 * <p>Seat state itself is never changed here. Holding, booking and cancelling stay
 * the responsibility of {@link Seat}, so the row only ever reports what it sees.</p>
 */
public class SeatRow {
    private final String id;
    private final String label;
    private final List<Seat> seats = new ArrayList<>();

    /**
     * @param id    stable identifier, unique within the owning section
     * @param label human readable row label such as {@code "A"} or {@code "12"}
     */
    public SeatRow(String id, String label) {
        if (id == null || id.isBlank() || label == null || label.isBlank()) {
            throw new IllegalArgumentException("Seat row id and label are required.");
        }
        this.id = id.trim();
        this.label = label.trim();
    }

    /**
     * Adds a seat to this row.
     *
     * @throws IllegalArgumentException if a seat with the same id or label is already present
     */
    public void addSeat(Seat seat) {
        Objects.requireNonNull(seat, "Seat cannot be null");
        boolean duplicate = seats.stream().anyMatch(existing ->
                existing.getId().equals(seat.getId())
                        || existing.getLabel().equalsIgnoreCase(seat.getLabel()));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate seat id or label in row " + label);
        }
        seats.add(seat);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    /** Total number of seats in this row, whatever their status. */
    public int getCapacity() {
        return seats.size();
    }

    public long getAvailableSeatCount() {
        return seats.stream().filter(Seat::isAvailable).count();
    }

    public Optional<Seat> findSeatById(String seatId) {
        if (seatId == null || seatId.isBlank()) {
            return Optional.empty();
        }
        return seats.stream().filter(seat -> seat.getId().equals(seatId.trim())).findFirst();
    }
}
