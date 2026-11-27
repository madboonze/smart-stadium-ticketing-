package com.smartstadium.ticketing;

import com.smartstadium.domain.booking.BookingStatus;
import com.smartstadium.domain.booking.Ticket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketModuleTest {
    private static final Instant CREATED = Instant.parse("2026-11-28T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(CREATED, ZoneOffset.UTC);
    private final TicketSigner signer = new TicketSigner("test-secret-0123456789");
    private final TicketGenerator generator = new TicketGenerator(signer, CLOCK);

    private static List<Ticket> twoSeats() {
        return List.of(
                new Ticket("A-1-1", "Stand A, Row 1, Seat 1", new BigDecimal("150.00")),
                new Ticket("A-1-2", "Stand A, Row 1, Seat 2", new BigDecimal("150.00")));
    }

    @Test
    void bookingReferenceIsReadableStableAndChecked() {
        BookingReference ref = BookingReference.forBooking("booking-123", CREATED);
        assertTrue(ref.value().startsWith("SS-261128-"));
        assertEquals(16, ref.value().length());
        assertEquals(ref, BookingReference.forBooking("booking-123", CREATED));
        assertTrue(BookingReference.isValid(ref.value().toLowerCase()));

        // Changing one character must break the check character.
        char last = ref.value().charAt(15);
        String typo = ref.value().substring(0, 15) + (last == '0' ? '1' : '0');
        assertFalse(BookingReference.isValid(typo));
        assertFalse(BookingReference.isValid("not-a-reference"));
    }

    @Test
    void issuesOneSignedTicketPerSeatForConfirmedBooking() {
        List<DigitalTicket> tickets = generator.generate("booking-123", CREATED, BookingStatus.CONFIRMED, twoSeats());
        assertEquals(2, tickets.size());
        assertEquals(tickets.get(0).bookingReference(), tickets.get(1).bookingReference());
        for (DigitalTicket t : tickets) {
            assertTrue(signer.verify(t.qrPayload()).isPresent());
            assertTrue(t.qrSvg().startsWith("<svg"));
        }
    }

    @Test
    void refusesToIssueTicketsForPendingOrCancelledBookings() {
        assertThrows(TicketGenerationException.class,
                () -> generator.generate("b", CREATED, BookingStatus.PENDING, twoSeats()));
        assertThrows(TicketGenerationException.class,
                () -> generator.generate("b", CREATED, BookingStatus.CANCELLED, twoSeats()));
        assertThrows(TicketGenerationException.class,
                () -> generator.generate("b", CREATED, BookingStatus.CONFIRMED, List.of()));
    }

    @Test
    void rejectsChangedOrForgedCodes() {
        String payload = generator.generate("b", CREATED, BookingStatus.CONFIRMED, twoSeats()).get(0).qrPayload();
        String changed = payload.replaceFirst("\\|[^|]+\\|", "|someone-elses-ticket|");
        assertFalse(signer.verify(changed).isPresent());
        assertFalse(new TicketSigner("a-different-secret-key").verify(payload).isPresent());
        assertFalse(signer.verify("hello").isPresent());
        assertFalse(signer.verify(null).isPresent());
    }

    @Test
    void ticketAdmitsOnlyOnce() {
        TicketRegistry registry = new TicketRegistry(signer, CLOCK);
        List<DigitalTicket> tickets = generator.generate("b", CREATED, BookingStatus.CONFIRMED, twoSeats());
        registry.register(tickets);
        String code = tickets.get(0).qrPayload();

        assertEquals(TicketScanResult.ADMITTED, registry.checkIn(code));
        assertEquals(TicketScanResult.ALREADY_USED, registry.checkIn(code));
        assertEquals(TicketScanResult.ADMITTED, registry.checkIn(tickets.get(1).qrPayload()));
        assertEquals(TicketScanResult.INVALID, registry.checkIn("SST1|fake|SS-261128-AAAAAA|xyz"));
        assertEquals(2, registry.findByBookingReference(tickets.get(0).bookingReference()).size());
    }

    @Test
    void ticketNotIssuedByThisServerIsUnknown() {
        TicketRegistry registry = new TicketRegistry(signer, CLOCK);
        String code = generator.generate("b", CREATED, BookingStatus.CONFIRMED, twoSeats()).get(0).qrPayload();
        assertEquals(TicketScanResult.UNKNOWN_TICKET, registry.checkIn(code));
    }

    @Test
    void qrCodeHasFinderPatternsInThreeCorners() {
        QrCode qr = QrCode.encode("SST1|example");
        int last = qr.getSize() - 1;
        // Each finder pattern: dark outer ring, light ring inside it, dark centre.
        int[][] corners = {{0, 0}, {last - 6, 0}, {0, last - 6}};
        for (int[] c : corners) {
            assertTrue(qr.isDark(c[0], c[1]));
            assertFalse(qr.isDark(c[0] + 1, c[1] + 1));
            assertTrue(qr.isDark(c[0] + 3, c[1] + 3));
        }
        assertThrows(IllegalArgumentException.class, () -> QrCode.encode("x".repeat(QrCode.MAX_BYTES + 1)));
    }
}
