package com.smartstadium.ticketing;

/** Thrown when tickets cannot be issued, for example because the booking is not confirmed yet. */
public class TicketGenerationException extends RuntimeException {
    public TicketGenerationException(String message) {
        super(message);
    }
}
