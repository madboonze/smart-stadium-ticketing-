package com.smartstadium.domain.seat;

import java.math.BigDecimal;

public final class RegularSeat extends Seat {
    public RegularSeat(String id, String label, BigDecimal basePrice) {
        super(id, label, basePrice);
    }

    public RegularSeat(String label, BigDecimal basePrice) {
        super(label, basePrice);
    }

    @Override
    public BigDecimal calculatePrice() {
        return getBasePrice();
    }

    @Override
    public SeatCategory getCategory() {
        return SeatCategory.REGULAR;
    }
}
