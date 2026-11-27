package com.smartstadium.ticketing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A ticket ready to show on a phone or print: seat details, the booking reference,
 * and the signed text that goes inside its QR code.
 * Built from the existing {@code Ticket} class without changing it.
 */
public record DigitalTicket(
        String ticketId,
        String bookingReference,
        String seatId,
        String seatLabel,
        BigDecimal price,
        Instant issuedAt,
        String qrPayload) {

    public DigitalTicket {
        if (ticketId == null || ticketId.isBlank()) throw new IllegalArgumentException("Ticket ID is required.");
        if (bookingReference == null || !BookingReference.isValid(bookingReference)) {
            throw new IllegalArgumentException("A valid booking reference is required.");
        }
        if (qrPayload == null || qrPayload.isBlank()) throw new IllegalArgumentException("QR payload is required.");
    }

    /** The QR code for this ticket as an SVG image. */
    public String qrSvg() {
        return QrCode.encode(qrPayload).toSvg(4);
    }
}
