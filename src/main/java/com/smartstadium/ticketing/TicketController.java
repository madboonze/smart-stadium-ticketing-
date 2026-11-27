package com.smartstadium.ticketing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Digital ticket endpoints:
 * <ul>
 *   <li>{@code GET  /api/tickets/{ticketId}}: ticket details</li>
 *   <li>{@code GET  /api/tickets/{ticketId}/qr}: the ticket's QR code as SVG</li>
 *   <li>{@code GET  /api/tickets/booking/{reference}}: all tickets for a booking reference</li>
 *   <li>{@code POST /api/tickets/check-in}: body {@code {"code": "..."}}, scans a ticket at the gate</li>
 *   <li>{@code POST /api/tickets/demo}: issues two sample tickets for demonstrations</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService service;
    private final TicketRegistry registry;

    public TicketController(TicketService service, TicketRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @GetMapping("/{ticketId}")
    public DigitalTicket ticket(@PathVariable("ticketId") String ticketId) {
        return registry.find(ticketId).orElseThrow(TicketController::notFound);
    }

    @GetMapping(value = "/{ticketId}/qr", produces = "image/svg+xml")
    public String qr(@PathVariable("ticketId") String ticketId) {
        return registry.find(ticketId).orElseThrow(TicketController::notFound).qrSvg();
    }

    @GetMapping("/booking/{reference}")
    public List<DigitalTicket> byBooking(@PathVariable("reference") String reference) {
        if (!BookingReference.isValid(reference)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid booking reference.");
        }
        return registry.findByBookingReference(reference);
    }

    @PostMapping("/check-in")
    public Map<String, Object> checkIn(@RequestBody Map<String, String> body) {
        String code = body == null ? null : body.get("code");
        TicketScanResult result = registry.checkIn(code);
        return Map.of("result", result.name(), "admitted", result == TicketScanResult.ADMITTED);
    }

    @PostMapping("/demo")
    public List<DigitalTicket> demo() {
        return service.issueDemo();
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found.");
    }
}
