package com.smartstadium.ticketing;

import com.smartstadium.domain.booking.Booking;
import com.smartstadium.domain.booking.BookingStatus;
import com.smartstadium.domain.booking.Ticket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The one entry point other modules need: call {@link #issue(Booking)} after a booking is confirmed.
 */
public class TicketService {
    private final TicketGenerator generator;
    private final TicketRegistry registry;

    public TicketService(TicketGenerator generator, TicketRegistry registry) {
        this.generator = generator;
        this.registry = registry;
    }

    /** Creates the digital tickets for a confirmed booking and remembers them for gate check-in. */
    public List<DigitalTicket> issue(Booking booking) {
        List<DigitalTicket> issued = generator.generate(booking);
        registry.register(issued);
        return issued;
    }

    /** Issues sample tickets so the feature can be shown before the booking workflow is finished. */
    public List<DigitalTicket> issueDemo() {
        List<Ticket> seats = List.of(
                new Ticket("DEMO-A-1-1", "Stand A, Row 1, Seat 1", new BigDecimal("150.00")),
                new Ticket("DEMO-A-1-2", "Stand A, Row 1, Seat 2", new BigDecimal("150.00")));
        List<DigitalTicket> issued = generator.generate(
                UUID.randomUUID().toString(), Instant.now(), BookingStatus.CONFIRMED, seats);
        registry.register(issued);
        return issued;
    }
}
