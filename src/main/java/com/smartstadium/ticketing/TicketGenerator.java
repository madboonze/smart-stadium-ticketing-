package com.smartstadium.ticketing;

import com.smartstadium.domain.booking.Booking;
import com.smartstadium.domain.booking.BookingStatus;
import com.smartstadium.domain.booking.Ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Turns a confirmed booking into digital tickets: one per seat, all sharing the booking reference,
 * each with its own signed QR payload.
 */
public class TicketGenerator {
    private final TicketSigner signer;
    private final Clock clock;

    public TicketGenerator(TicketSigner signer, Clock clock) {
        if (signer == null || clock == null) throw new IllegalArgumentException("Signer and clock are required.");
        this.signer = signer;
        this.clock = clock;
    }

    /** Issues tickets for a booking. The booking must be confirmed. */
    public List<DigitalTicket> generate(Booking booking) {
        if (booking == null) throw new TicketGenerationException("Booking is required.");
        return generate(booking.getId(), booking.getCreatedAt(), booking.getStatus(), booking.getTickets());
    }

    /** Same as {@link #generate(Booking)}, using the booking's parts directly. */
    public List<DigitalTicket> generate(String bookingId, Instant createdAt, BookingStatus status, List<Ticket> tickets) {
        if (status != BookingStatus.CONFIRMED) {
            throw new TicketGenerationException("Tickets can only be issued for a confirmed booking (status was " + status + ").");
        }
        if (tickets == null || tickets.isEmpty()) {
            throw new TicketGenerationException("A confirmed booking must have at least one ticket.");
        }
        BookingReference reference = BookingReference.forBooking(bookingId, createdAt);
        Instant issuedAt = clock.instant();
        return tickets.stream()
                .map(t -> new DigitalTicket(
                        t.getId(),
                        reference.value(),
                        t.getSeatId(),
                        t.getSeatLabel(),
                        t.getPrice(),
                        issuedAt,
                        signer.sign(t.getId(), reference)))
                .toList();
    }
}
