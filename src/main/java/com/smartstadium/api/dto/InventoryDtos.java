package com.smartstadium.api.dto;

import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.stadium.SeatRow;
import com.smartstadium.domain.stadium.Section;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read models for the seat inventory API.
 *
 * <p>These records exist so the REST layer never serialises the domain objects directly.
 * That matters for the seat map: the browser gets a stable, flat shape it can render,
 * and the domain classes stay free to change without breaking the frontend.</p>
 *
 * <p>Each record exposes a static {@code of} factory so the mapping lives next to the
 * shape it produces rather than inside a controller.</p>
 */
public final class InventoryDtos {

    private InventoryDtos() {
        // Container for the view records only.
    }

    /** A single seat, as the seat map needs it. */
    public record SeatView(
            String id,
            String label,
            String category,
            String status,
            BigDecimal price) {

        public static SeatView of(Seat seat) {
            return new SeatView(
                    seat.getId(),
                    seat.getLabel(),
                    seat.getCategory().name(),
                    seat.getStatus().name(),
                    seat.calculatePrice());
        }
    }

    /** One row of seats. */
    public record RowView(
            String id,
            String label,
            int capacity,
            long availableSeats,
            List<SeatView> seats) {

        public static RowView of(SeatRow row) {
            return new RowView(
                    row.getId(),
                    row.getLabel(),
                    row.getCapacity(),
                    row.getAvailableSeatCount(),
                    row.getSeats().stream().map(SeatView::of).toList());
        }
    }

    /**
     * A seating section. {@code rows} is {@code null} when the caller asked for the
     * overview only, which keeps the stadium-wide response small; request the seats
     * explicitly to have it populated.
     */
    public record SectionView(
            String id,
            String code,
            String name,
            String category,
            BigDecimal basePrice,
            int capacity,
            long availableSeats,
            int rowCount,
            List<RowView> rows) {

        public static SectionView of(Section section, boolean includeSeats) {
            return new SectionView(
                    section.getId(),
                    section.getCode(),
                    section.getName(),
                    section.getCategory().name(),
                    section.getBasePrice(),
                    section.getCapacity(),
                    section.getAvailableSeatCount(),
                    section.getRows().size(),
                    includeSeats
                            ? section.getRows().stream().map(RowView::of).toList()
                            : null);
        }
    }

    /** A stand and the sections inside it. */
    public record StandView(
            String id,
            String name,
            int capacity,
            long availableSeats,
            List<SectionView> sections) {

        public static StandView of(Stand stand, boolean includeSeats) {
            return new StandView(
                    stand.getId(),
                    stand.getName(),
                    stand.getCapacity(),
                    stand.getAvailableSeatCount(),
                    stand.getSections().stream()
                            .map(section -> SectionView.of(section, includeSeats))
                            .toList());
        }
    }

    /** The full stadium tree, which is what the 3D seat map loads first. */
    public record StadiumInventoryView(
            String id,
            String name,
            String city,
            int capacity,
            long availableSeats,
            List<StandView> stands) {

        public static StadiumInventoryView of(Stadium stadium, boolean includeSeats) {
            return new StadiumInventoryView(
                    stadium.getId(),
                    stadium.getName(),
                    stadium.getCity(),
                    stadium.getStands().stream().mapToInt(Stand::getCapacity).sum(),
                    stadium.getAvailableSeatCount(),
                    stadium.getStands().stream()
                            .map(stand -> StandView.of(stand, includeSeats))
                            .toList());
        }
    }

    /** One line per stadium, for the stadium picker. */
    public record StadiumSummaryView(
            String id,
            String name,
            String city,
            int capacity,
            long availableSeats,
            int standCount) {

        public static StadiumSummaryView of(Stadium stadium) {
            return new StadiumSummaryView(
                    stadium.getId(),
                    stadium.getName(),
                    stadium.getCity(),
                    stadium.getStands().stream().mapToInt(Stand::getCapacity).sum(),
                    stadium.getAvailableSeatCount(),
                    stadium.getStands().size());
        }
    }
}
