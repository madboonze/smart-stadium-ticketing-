package com.smartstadium.persistence;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.match.MatchStatus;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.SeatCategory;
import com.smartstadium.domain.seat.SeatStatus;
import com.smartstadium.domain.stadium.SeatFactory;
import com.smartstadium.domain.stadium.SeatRow;
import com.smartstadium.domain.stadium.Section;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import com.smartstadium.persistence.entity.MatchEntity;
import com.smartstadium.persistence.entity.SeatEntity;
import com.smartstadium.persistence.entity.SeatRowEntity;
import com.smartstadium.persistence.entity.SectionEntity;
import com.smartstadium.persistence.entity.StadiumEntity;
import com.smartstadium.persistence.entity.StandEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts between the domain model and the database rows, in both directions.
 *
 * <p>This class is the reason the domain classes needed no changes for Module 6. It is
 * plain Java with no framework dependencies, so it can be unit tested on its own, and it
 * rebuilds domain objects using only their public API.</p>
 *
 * <p>Restoring a seat's status is the interesting part. {@code Seat} has no status setter
 * by design: the only way in is {@code hold()}, {@code confirmBooking()} and friends. So a
 * seat that was BOOKED is rebuilt by replaying those transitions in order, which means the
 * domain's own invariants are enforced on the way back out of the database rather than
 * bypassed by reflection.</p>
 */
public final class InventoryMapper {

    private InventoryMapper() {
        // Static mapper.
    }

    /**
     * One stadium aggregate as flat database rows, which is how it is read and written.
     * The schema's foreign keys hold the shape together; {@link #assemble(StadiumRows)}
     * puts the tree back together in Java.
     */
    public record StadiumRows(
            StadiumEntity stadium,
            List<StandEntity> stands,
            List<SectionEntity> sections,
            List<SeatRowEntity> rows,
            List<SeatEntity> seats) {
    }

    /** One section's rows, for writing a newly created section without touching the rest. */
    public record SectionRows(
            SectionEntity section,
            List<SeatRowEntity> rows,
            List<SeatEntity> seats) {
    }

    /** One stand's rows, including any sections it already holds. */
    public record StandRows(
            StandEntity stand,
            List<SectionEntity> sections,
            List<SeatRowEntity> rows,
            List<SeatEntity> seats) {
    }

    // --- domain to rows -------------------------------------------------------------

    public static StadiumEntity toEntity(Stadium stadium) {
        return new StadiumEntity(stadium.getId(), stadium.getName(), stadium.getCity());
    }

    /** Flattens one section and everything inside it. */
    public static SectionRows flattenSection(String standId, Section section, int ordinal) {
        List<SeatRowEntity> rowRows = new ArrayList<>();
        List<SeatEntity> seatRows = new ArrayList<>();

        List<SeatRow> rows = section.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            SeatRow row = rows.get(rowIndex);
            rowRows.add(new SeatRowEntity(
                    row.getId(), section.getId(), row.getLabel(), rowIndex));

            List<Seat> seats = row.getSeats();
            for (int seatIndex = 0; seatIndex < seats.size(); seatIndex++) {
                seatRows.add(seatRow(seats.get(seatIndex), row.getId(), null, seatIndex));
            }
        }

        SectionEntity sectionRow = new SectionEntity(
                section.getId(),
                standId,
                section.getCode(),
                section.getName(),
                section.getCategory().name(),
                section.getBasePrice().toPlainString(),
                ordinal);
        return new SectionRows(sectionRow, rowRows, seatRows);
    }

    /** Flattens one stand, its directly attached seats, and all of its sections. */
    public static StandRows flattenStand(String stadiumId, Stand stand, int ordinal) {
        List<SectionEntity> sectionRows = new ArrayList<>();
        List<SeatRowEntity> rowRows = new ArrayList<>();
        List<SeatEntity> seatRows = new ArrayList<>();

        // Seats attached straight to the stand carry stand_id and no row_id.
        List<Seat> directSeats = stand.getSeats();
        for (int seatIndex = 0; seatIndex < directSeats.size(); seatIndex++) {
            seatRows.add(seatRow(directSeats.get(seatIndex), null, stand.getId(), seatIndex));
        }

        List<Section> sections = stand.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            SectionRows flattened =
                    flattenSection(stand.getId(), sections.get(sectionIndex), sectionIndex);
            sectionRows.add(flattened.section());
            rowRows.addAll(flattened.rows());
            seatRows.addAll(flattened.seats());
        }

        StandEntity standRow =
                new StandEntity(stand.getId(), stadiumId, stand.getName(), ordinal);
        return new StandRows(standRow, sectionRows, rowRows, seatRows);
    }

    /** Flattens a whole stadium, including every section, row and seat, into rows. */
    public static StadiumRows flatten(Stadium stadium) {
        List<StandEntity> standRows = new ArrayList<>();
        List<SectionEntity> sectionRows = new ArrayList<>();
        List<SeatRowEntity> rowRows = new ArrayList<>();
        List<SeatEntity> seatRows = new ArrayList<>();

        List<Stand> stands = stadium.getStands();
        for (int standIndex = 0; standIndex < stands.size(); standIndex++) {
            StandRows flattened =
                    flattenStand(stadium.getId(), stands.get(standIndex), standIndex);
            standRows.add(flattened.stand());
            sectionRows.addAll(flattened.sections());
            rowRows.addAll(flattened.rows());
            seatRows.addAll(flattened.seats());
        }

        return new StadiumRows(toEntity(stadium), standRows, sectionRows, rowRows, seatRows);
    }

    private static SeatEntity seatRow(Seat seat, String rowId, String standId, int ordinal) {
        return new SeatEntity(
                seat.getId(),
                rowId,
                standId,
                seat.getLabel(),
                seat.getCategory().name(),
                seat.getBasePrice().toPlainString(),
                seat.getStatus().name(),
                ordinal);
    }

    public static MatchEntity toEntity(Match match) {
        return new MatchEntity(
                match.getId(),
                match.getStadium().getId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getKickoff().toString(),
                match.getStatus().name());
    }

    // --- rows to domain -------------------------------------------------------------

    /**
     * Rebuilds a stadium and everything inside it from flat rows, restoring seat statuses
     * by replaying the domain's own state transitions.
     */
    public static Stadium assemble(StadiumRows rows) {
        Stadium stadium = new Stadium(
                rows.stadium().getId(), rows.stadium().getName(), rows.stadium().getCity());

        Map<String, List<SectionEntity>> sectionsByStand = rows.sections().stream()
                .collect(Collectors.groupingBy(SectionEntity::getStandId));
        Map<String, List<SeatRowEntity>> rowsBySection = rows.rows().stream()
                .collect(Collectors.groupingBy(SeatRowEntity::getSectionId));
        Map<String, List<SeatEntity>> seatsByRow = rows.seats().stream()
                .filter(seat -> seat.getRowId() != null)
                .collect(Collectors.groupingBy(SeatEntity::getRowId));
        Map<String, List<SeatEntity>> seatsByStand = rows.seats().stream()
                .filter(seat -> seat.getRowId() == null && seat.getStandId() != null)
                .collect(Collectors.groupingBy(SeatEntity::getStandId));

        for (StandEntity standRow : sorted(rows.stands(), StandEntity::getOrdinal)) {
            Stand stand = new Stand(standRow.getId(), standRow.getName());

            for (SeatEntity seatRow : sorted(
                    seatsByStand.getOrDefault(standRow.getId(), List.of()), SeatEntity::getOrdinal)) {
                stand.addSeat(toSeat(seatRow));
            }

            for (SectionEntity sectionRow : sorted(
                    sectionsByStand.getOrDefault(standRow.getId(), List.of()),
                    SectionEntity::getOrdinal)) {
                Section section = new Section(
                        sectionRow.getId(),
                        sectionRow.getCode(),
                        sectionRow.getName(),
                        SeatCategory.valueOf(sectionRow.getCategory()),
                        new BigDecimal(sectionRow.getBasePrice()));

                for (SeatRowEntity seatRowRow : sorted(
                        rowsBySection.getOrDefault(sectionRow.getId(), List.of()),
                        SeatRowEntity::getOrdinal)) {
                    SeatRow row = new SeatRow(seatRowRow.getId(), seatRowRow.getLabel());
                    for (SeatEntity seatRow : sorted(
                            seatsByRow.getOrDefault(seatRowRow.getId(), List.of()),
                            SeatEntity::getOrdinal)) {
                        row.addSeat(toSeat(seatRow));
                    }
                    section.addRow(row);
                }
                stand.addSection(section);
            }
            stadium.addStand(stand);
        }
        return stadium;
    }

    /** Rebuilds one seat, including its status, through the domain's own transitions. */
    public static Seat toSeat(SeatEntity row) {
        Seat seat = SeatFactory.create(
                SeatCategory.valueOf(row.getCategory()),
                row.getId(),
                row.getLabel(),
                new BigDecimal(row.getBasePrice()));
        applyStatus(seat, SeatStatus.valueOf(row.getStatus()));
        return seat;
    }

    private static void applyStatus(Seat seat, SeatStatus status) {
        switch (status) {
            case AVAILABLE -> {
                // A freshly built seat is already available.
            }
            case HELD -> seat.hold();
            case BOOKED -> {
                seat.hold();
                seat.confirmBooking();
            }
        }
    }

    /**
     * Rebuilds a match against an already assembled stadium.
     *
     * <p>{@code SOLD_OUT} and {@code COMPLETED} exist in {@code MatchStatus} but {@code Match}
     * exposes no transition that reaches them, so nothing in the application can currently
     * produce or restore them; such a row comes back as {@code SCHEDULED}. When Module 3
     * adds those transitions, this switch should gain the matching cases.</p>
     */
    public static Match toMatch(MatchEntity row, Stadium stadium) {
        Match match = new Match(
                row.getId(),
                row.getHomeTeam(),
                row.getAwayTeam(),
                LocalDateTime.parse(row.getKickoff()),
                stadium);
        switch (MatchStatus.valueOf(row.getStatus())) {
            case SCHEDULED, SOLD_OUT, COMPLETED -> {
                // SCHEDULED is the constructed state; the other two are unreachable today.
            }
            case ON_SALE -> match.putOnSale();
            case CANCELLED -> match.cancel();
        }
        return match;
    }

    private static <T> List<T> sorted(List<T> items, java.util.function.ToIntFunction<T> ordinal) {
        return items.stream().sorted(Comparator.comparingInt(ordinal)).toList();
    }
}
