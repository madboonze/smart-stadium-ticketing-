package com.smartstadium.domain.booking;

import java.math.BigDecimal;
import java.util.UUID;

public class Ticket {
    private final String id;
    private final String seatId;
    private final String seatLabel;
    private final BigDecimal price;

    public Ticket(String seatId, String seatLabel, BigDecimal price) {
        if (seatId == null || seatId.isBlank() || seatLabel == null || seatLabel.isBlank()) {
            throw new IllegalArgumentException("Ticket seat details are required.");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Ticket price must be positive.");
        }
        this.id = UUID.randomUUID().toString();
        this.seatId = seatId;
        this.seatLabel = seatLabel;
        this.price = price;
    }

    public String getId() { return id; }
    public String getSeatId() { return seatId; }
    public String getSeatLabel() { return seatLabel; }
    public BigDecimal getPrice() { return price; }
}
