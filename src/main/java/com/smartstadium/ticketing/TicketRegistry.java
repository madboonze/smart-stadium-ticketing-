package com.smartstadium.ticketing;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps issued tickets in memory and records gate check-ins, so each ticket admits one person once.
 * Like the other modules, it is in-memory until the database module replaces it. Data resets on restart.
 */
public class TicketRegistry {
    private final TicketSigner signer;
    private final Clock clock;
    private final Map<String, DigitalTicket> tickets = new ConcurrentHashMap<>();
    private final Map<String, Instant> checkIns = new ConcurrentHashMap<>();

    public TicketRegistry(TicketSigner signer, Clock clock) {
        if (signer == null || clock == null) throw new IllegalArgumentException("Signer and clock are required.");
        this.signer = signer;
        this.clock = clock;
    }

    public void register(List<DigitalTicket> issued) {
        issued.forEach(t -> tickets.put(t.ticketId(), t));
    }

    public Optional<DigitalTicket> find(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    public List<DigitalTicket> findByBookingReference(String reference) {
        if (!BookingReference.isValid(reference)) return List.of();
        String normalised = BookingReference.parse(reference).value();
        return tickets.values().stream()
                .filter(t -> t.bookingReference().equals(normalised))
                .toList();
    }

    /** Checks a scanned QR payload and, if it is genuine and unused, marks it as used. */
    public TicketScanResult checkIn(String payload) {
        Optional<TicketSigner.ScannedTicket> scanned = signer.verify(payload);
        if (scanned.isEmpty()) return TicketScanResult.INVALID;

        DigitalTicket ticket = tickets.get(scanned.get().ticketId());
        if (ticket == null || !ticket.bookingReference().equals(scanned.get().bookingReference().value())) {
            return TicketScanResult.UNKNOWN_TICKET;
        }
        // putIfAbsent is atomic, so two gates scanning the same ticket at once cannot both admit it.
        Instant previous = checkIns.putIfAbsent(ticket.ticketId(), clock.instant());
        return previous == null ? TicketScanResult.ADMITTED : TicketScanResult.ALREADY_USED;
    }

    public Optional<Instant> checkedInAt(String ticketId) {
        return Optional.ofNullable(checkIns.get(ticketId));
    }
}
