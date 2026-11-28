package com.smartstadium.domain.stadium;

import com.smartstadium.domain.seat.Seat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class Stand {
    private final String id;
    private final String name;
    private final List<Seat> seats = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();

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
        return getAllSeats().stream().filter(Seat::isAvailable).count();
    }

    // --- Module 2: sections and rows -------------------------------------------------
    //
    // A stand can hold seats in two ways. Seats added through addSeat sit directly on the
    // stand, which is how the original demo fixture works. Seats added through a Section
    // sit inside a Section -> SeatRow -> Seat structure, which is what the seat map and the
    // booking engine use. The two never hold the same Seat instance, so counts never
    // double up: getSeats returns only the directly attached seats, exactly as before,
    // while getAllSeats returns both groups together.

    /**
     * Adds a seating section to this stand.
     *
     * @throws IllegalArgumentException if a section with the same id or code already exists
     */
    public void addSection(Section section) {
        Objects.requireNonNull(section, "Section cannot be null");
        boolean duplicate = sections.stream().anyMatch(existing ->
                existing.getId().equals(section.getId())
                        || existing.getCode().equalsIgnoreCase(section.getCode()));
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Duplicate section id or code in stand " + name + ": " + section.getCode());
        }
        sections.add(section);
    }

    public List<Section> getSections() {
        return Collections.unmodifiableList(sections);
    }

    /** Seats attached directly to this stand plus every seat held inside its sections. */
    public List<Seat> getAllSeats() {
        return Stream.concat(
                        seats.stream(),
                        sections.stream().flatMap(section -> section.getSeats().stream()))
                .toList();
    }

    /** Total number of seats in this stand, whatever their status. */
    public int getCapacity() {
        return seats.size() + sections.stream().mapToInt(Section::getCapacity).sum();
    }

    public Optional<Section> findSectionById(String sectionId) {
        if (sectionId == null || sectionId.isBlank()) {
            return Optional.empty();
        }
        return sections.stream()
                .filter(section -> section.getId().equals(sectionId.trim()))
                .findFirst();
    }

    public Optional<Section> findSectionByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return sections.stream()
                .filter(section -> section.getCode().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
