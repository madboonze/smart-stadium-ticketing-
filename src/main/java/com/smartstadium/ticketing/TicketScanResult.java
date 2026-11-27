package com.smartstadium.ticketing;

/** What happens when a ticket's QR code is scanned at the gate. */
public enum TicketScanResult {
    /** Genuine ticket, first scan: let the person in. */
    ADMITTED,
    /** Genuine ticket that has already been scanned once. */
    ALREADY_USED,
    /** The signature is correct but this server has no record of the ticket. */
    UNKNOWN_TICKET,
    /** Not a Smart Stadium ticket, or the code was changed. */
    INVALID
}
