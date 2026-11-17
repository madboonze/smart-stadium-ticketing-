package com.smartstadium.domain.seat;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeatPricingTest {
    @Test
    void eachSeatTypeCalculatesItsOwnPrice() {
        Seat regular = new RegularSeat("R1", new BigDecimal("1000"));
        Seat premium = new PremiumSeat("P1", new BigDecimal("1000"));
        Seat vip = new VIPSeat("V1", new BigDecimal("1000"));

        assertEquals(new BigDecimal("1000"), regular.calculatePrice());
        assertEquals(new BigDecimal("1500.00"), premium.calculatePrice());
        assertEquals(new BigDecimal("2500.00"), vip.calculatePrice());
    }
}
