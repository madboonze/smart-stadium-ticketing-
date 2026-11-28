package com.smartstadium.domain.stadium;

import com.smartstadium.domain.seat.PremiumSeat;
import com.smartstadium.domain.seat.RegularSeat;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.SeatCategory;
import com.smartstadium.domain.seat.VIPSeat;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Creates the correct {@link Seat} subclass for a {@link SeatCategory}.
 *
 * <p>This keeps the category-to-class decision in exactly one place instead of
 * repeating {@code if/else} chains wherever seats are generated. The pricing
 * behaviour still belongs to the seat subclasses themselves: {@link RegularSeat}
 * charges the base price, {@link PremiumSeat} and {@link VIPSeat} apply their own
 * multipliers. Nothing about those rules is duplicated here.</p>
 */
public final class SeatFactory {

    private SeatFactory() {
        // Utility class, never instantiated.
    }

    /**
     * Builds a seat of the requested category.
     *
     * @param category  which kind of seat to create
     * @param id        stable seat identifier
     * @param label     human readable seat label
     * @param basePrice the section's base price, before the category multiplier
     */
    public static Seat create(SeatCategory category, String id, String label, BigDecimal basePrice) {
        Objects.requireNonNull(category, "Seat category is required");
        return switch (category) {
            case REGULAR -> new RegularSeat(id, label, basePrice);
            case PREMIUM -> new PremiumSeat(id, label, basePrice);
            case VIP -> new VIPSeat(id, label, basePrice);
        };
    }
}
