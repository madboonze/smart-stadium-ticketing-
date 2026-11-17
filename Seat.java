package com.smartstadium.domain.seat;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Base type for every seat in the stadium.
 * State is private and changes only through domain methods.
 */
public abstract class Seat implements Priceable {
    private final String id;
    private final String label;
    private final BigDecimal basePrice;
    private SeatStatus status;

    protected Seat(String id, String label, BigDecimal basePrice) {
        this.id = requireText(id, "Seat id");
        this.label = requireText(label, "Seat label");
        this.basePrice = requirePositivePrice(basePrice);
        this.status = SeatStatus.AVAILABLE;
    }

    protected Seat(String label, BigDecimal basePrice) {
        this(UUID.randomUUID().toString(), label, basePrice);
    }

    public abstract SeatCategory getCategory();

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public void hold() {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat " + label + " is not available.");
        }
        status = SeatStatus.HELD;
    }

    public void releaseHold() {
        if (status != SeatStatus.HELD) {
            throw new IllegalStateException("Seat " + label + " is not currently held.");
        }
        status = SeatStatus.AVAILABLE;
    }

    public void confirmBooking() {
        if (status != SeatStatus.HELD) {
            throw new IllegalStateException("Seat " + label + " must be held before booking.");
        }
        status = SeatStatus.BOOKED;
    }

    public void cancelBooking() {
        if (status != SeatStatus.BOOKED) {
            throw new IllegalStateException("Seat " + label + " is not booked.");
        }
        status = SeatStatus.AVAILABLE;
    }

    protected static BigDecimal requirePositivePrice(BigDecimal price) {
        Objects.requireNonNull(price, "Base price is required");
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Base price must be greater than zero.");
        }
        return price;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank.");
        }
        return value.trim();
    }
}
