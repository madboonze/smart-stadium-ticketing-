package com.smartstadium.ticketing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Creates and checks the text stored inside each ticket's QR code.
 * <p>
 * Format: {@code SST1|<ticket id>|<booking reference>|<signature>}.
 * The signature is an HMAC-SHA256 of the first three parts, made with a secret key
 * that only the server knows, so a copied or hand-made code with changed details is rejected.
 */
public final class TicketSigner {
    static final String PREFIX = "SST1";
    private static final int SIGNATURE_BYTES = 12;

    private final byte[] key;

    public TicketSigner(String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("Ticket secret must be at least 16 characters.");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Builds the signed QR payload for one ticket. */
    public String sign(String ticketId, BookingReference reference) {
        if (ticketId == null || ticketId.isBlank() || ticketId.contains("|")) {
            throw new IllegalArgumentException("Ticket ID is required and must not contain '|'.");
        }
        String body = PREFIX + "|" + ticketId + "|" + reference.value();
        return body + "|" + signature(body);
    }

    /** Reads a scanned payload. Returns empty if the format is wrong or the signature does not match. */
    public Optional<ScannedTicket> verify(String payload) {
        if (payload == null) return Optional.empty();
        String[] parts = payload.trim().split("\\|", -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0]) || parts[1].isBlank()
                || !BookingReference.isValid(parts[2])) {
            return Optional.empty();
        }
        String body = parts[0] + "|" + parts[1] + "|" + parts[2];
        byte[] expected = signature(body).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = parts[3].getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) { // constant-time comparison
            return Optional.empty();
        }
        return Optional.of(new ScannedTicket(parts[1], BookingReference.parse(parts[2])));
    }

    private String signature(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] full = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(full, SIGNATURE_BYTES));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available.", e);
        }
    }

    /** The trusted details read from a QR code whose signature was correct. */
    public record ScannedTicket(String ticketId, BookingReference bookingReference) { }
}
