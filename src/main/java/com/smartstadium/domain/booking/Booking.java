package com.smartstadium.domain.booking;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Booking implements Bookable {
    private final String id;
    private final User customer;
    private final Match match;
    private final List<Seat> seats;
    private final List<Ticket> tickets = new ArrayList<>();
    private final Instant createdAt;
    private BookingStatus status;

    public Booking(User customer, Match match, List<Seat> seats) {
        if (customer == null || match == null || seats == null || seats.isEmpty()) {
            throw new IllegalArgumentException("Customer, match and at least one seat are required.");
        }
        if (seats.stream().anyMatch(seat -> seat == null || !seat.isAvailable())) {
            throw new IllegalArgumentException("Every selected seat must be available.");
        }
        this.id = UUID.randomUUID().toString();
        this.customer = customer;
        this.match = match;
        this.seats = new ArrayList<>(seats);
        this.createdAt = Instant.now();
        this.status = BookingStatus.PENDING;

        // Reserve selected seats immediately. Later modules add expiry and transactional persistence.
        this.seats.forEach(Seat::hold);
    }

    @Override
    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be confirmed.");
        }
        seats.forEach(Seat::confirmBooking);
        tickets.clear();
        seats.forEach(seat -> tickets.add(
                new Ticket(seat.getId(), seat.getLabel(), seat.calculatePrice())));
        status = BookingStatus.CONFIRMED;
    }

    @Override
    public void cancel() {
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("This booking cannot be cancelled.");
        }
        if (status == BookingStatus.PENDING) {
            seats.forEach(Seat::releaseHold);
        } else {
            seats.forEach(Seat::cancelBooking);
        }
        status = BookingStatus.CANCELLED;
    }

    public BigDecimal getTotal() {
        return seats.stream()
                .map(Seat::calculatePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getId() { return id; }
    public User getCustomer() { return customer; }
    public Match getMatch() { return match; }
    public List<Seat> getSeats() { return Collections.unmodifiableList(seats); }
    public List<Ticket> getTickets() { return Collections.unmodifiableList(tickets); }
    public Instant getCreatedAt() { return createdAt; }
    public BookingStatus getStatus() { return status; }
}
