package com.smartstadium.ticketing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * A short, human-friendly booking ID such as {@code SS-261128-7KQ4XA}.
 * <p>
 * Format: {@code SS-<yyMMdd>-<5 code characters><1 check character>}.
 * The code uses Crockford base32 (no I, L, O or U), so it is easy to read out loud.
 * The same booking always gets the same reference, and the check character catches typing mistakes.
 */
public final class BookingReference {
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final Pattern FORMAT = Pattern.compile("SS-\\d{6}-[0-9A-HJKMNP-TV-Z]{6}");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);

    private final String value;

    private BookingReference(String value) {
        this.value = value;
    }

    /** Builds the reference for a booking from its internal ID and creation time. */
    public static BookingReference forBooking(String bookingId, Instant createdAt) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("Booking ID is required.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Booking creation time is required.");
        }
        byte[] hash = sha256(bookingId);
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 5; i++) {
            code.append(ALPHABET.charAt((hash[i] & 0xFF) % 32));
        }
        String date = DATE.format(createdAt);
        code.append(checkChar(date + code));
        return new BookingReference("SS-" + date + "-" + code);
    }

    /** Parses a reference typed or scanned by a person. Lowercase is accepted. */
    public static BookingReference parse(String text) {
        if (!isValid(text)) {
            throw new IllegalArgumentException("Not a valid booking reference: " + text);
        }
        return new BookingReference(text.trim().toUpperCase());
    }

    /** True if the text has the right format and a correct check character. */
    public static boolean isValid(String text) {
        if (text == null) return false;
        String s = text.trim().toUpperCase();
        if (!FORMAT.matcher(s).matches()) return false;
        String date = s.substring(3, 9);
        String code = s.substring(10, 15);
        return s.charAt(15) == checkChar(date + code);
    }

    private static char checkChar(String body) {
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            int v = Character.isDigit(body.charAt(i)) ? body.charAt(i) - '0' : ALPHABET.indexOf(body.charAt(i));
            sum += v * (i + 1);
        }
        return ALPHABET.charAt(sum % 32);
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BookingReference other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
