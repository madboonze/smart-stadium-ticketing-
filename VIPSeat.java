package com.smartstadium.domain.seat;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class VIPSeat extends Seat {
    private static final BigDecimal MULTIPLIER = new BigDecimal("2.50");

    public VIPSeat(String id, String label, BigDecimal basePrice) {
        super(id, label, basePrice);
    }

    public VIPSeat(String label, BigDecimal basePrice) {
        super(label, basePrice);
    }

    @Override
    public BigDecimal calculatePrice() {
        return getBasePrice().multiply(MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public SeatCategory getCategory() {
        return SeatCategory.VIP;
    }
}
