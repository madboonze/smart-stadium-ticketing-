package com.smartstadium.persistence;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.match.MatchStatus;
import com.smartstadium.domain.seat.PremiumSeat;
import com.smartstadium.domain.seat.RegularSeat;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.SeatCategory;
import com.smartstadium.domain.seat.SeatStatus;
import com.smartstadium.domain.seat.VIPSeat;
import com.smartstadium.domain.stadium.SeatRow;
import com.smartstadium.domain.stadium.Section;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import com.smartstadium.persistence.InventoryMapper.SectionRows;
import com.smartstadium.persistence.InventoryMapper.StadiumRows;
import com.smartstadium.persistence.entity.MatchEntity;
import com.smartstadium.persistence.entity.SeatEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module 6: the domain is rebuilt from database rows without losing anything, and
 * without the domain classes needing to know the database exists.
 */
class InventoryMapperTest {

    /** A stadium using both ways a stand can hold seats, with mixed statuses. */
    private static Stadium sampleStadium() {
        Stadium stadium = new Stadium("stadium-1", "Dubai Grand Arena", "Dubai");

        Stand north = new Stand("stand-north", "North Stand");
        Section lower = new Section("sec-n1", "N1", "North Lower Tier",
                SeatCategory.REGULAR, new BigDecimal("150"));
        lower.generateRows(3, 4);
        Section vip = new Section("sec-n3", "N3", "North Executive Box",
                SeatCategory.VIP, new BigDecimal("99.50"));
        vip.generateRows(2, 2);
        north.addSection(lower);
        north.addSection(vip);

        Stand legacy = new Stand("stand-legacy", "Legacy Stand");
        legacy.addSeat(new RegularSeat("L-1", "L-1", new BigDecimal("100")));
        legacy.addSeat(new PremiumSeat("L-2", "L-2", new BigDecimal("100.25")));

        stadium.addStand(north);
        stadium.addStand(legacy);
        return stadium;
    }

    private static Stadium roundTrip(Stadium stadium) {
        return InventoryMapper.assemble(InventoryMapper.flatten(stadium));
    }

    @Test
    void stadiumSurvivesARoundTrip() {
        Stadium restored = roundTrip(sampleStadium());

        assertEquals("stadium-1", restored.getId());
        assertEquals("Dubai Grand Arena", restored.getName());
        assertEquals("Dubai", restored.getCity());
        assertEquals(2, restored.getStands().size());
    }

    @Test
    void standOrderAndIdentityArePreserved() {
        Stadium restored = roundTrip(sampleStadium());

        assertEquals(List.of("stand-north", "stand-legacy"),
                restored.getStands().stream().map(Stand::getId).toList());
        assertEquals(List.of("North Stand", "Legacy Stand"),
                restored.getStands().stream().map(Stand::getName).toList());
    }

    @Test
    void sectionsRowsAndSeatsAreRebuiltInOrder() {
        Stand north = roundTrip(sampleStadium()).getStands().get(0);

        assertEquals(List.of("N1", "N3"),
                north.getSections().stream().map(Section::getCode).toList());

        Section lower = north.findSectionByCode("N1").orElseThrow();
        assertEquals(3, lower.getRows().size());
        assertEquals(12, lower.getCapacity());
        assertEquals(List.of("A", "B", "C"),
                lower.getRows().stream().map(SeatRow::getLabel).toList());
        assertEquals(List.of("N1-A-01", "N1-A-02", "N1-A-03", "N1-A-04"),
                lower.getRows().get(0).getSeats().stream().map(Seat::getLabel).toList());
    }

    @Test
    void seatsAttachedDirectlyToAStandStayThere() {
        Stand legacy = roundTrip(sampleStadium()).getStands().get(1);

        assertEquals(2, legacy.getSeats().size());
        assertTrue(legacy.getSections().isEmpty());
        assertEquals(List.of("L-1", "L-2"),
                legacy.getSeats().stream().map(Seat::getId).toList());
        assertEquals(2, legacy.getCapacity());
    }

    @Test
    void seatCategoriesComeBackAsTheRightSubclass() {
        Stadium restored = roundTrip(sampleStadium());
        Stand north = restored.getStands().get(0);
        Stand legacy = restored.getStands().get(1);

        assertInstanceOf(RegularSeat.class,
                north.findSectionByCode("N1").orElseThrow().getSeats().get(0));
        assertInstanceOf(VIPSeat.class,
                north.findSectionByCode("N3").orElseThrow().getSeats().get(0));
        assertInstanceOf(RegularSeat.class, legacy.getSeats().get(0));
        assertInstanceOf(PremiumSeat.class, legacy.getSeats().get(1));
    }

    @Test
    void pricesKeepTheirExactDecimalValue() {
        Stadium restored = roundTrip(sampleStadium());
        Stand north = restored.getStands().get(0);

        Section vip = north.findSectionByCode("N3").orElseThrow();
        assertEquals(0, vip.getBasePrice().compareTo(new BigDecimal("99.50")));
        // 99.50 x 2.5 = 248.75, which a floating point column would not round-trip exactly.
        assertEquals(0, vip.getSeats().get(0).calculatePrice().compareTo(new BigDecimal("248.75")));

        Seat premium = restored.getStands().get(1).getSeats().get(1);
        assertEquals(0, premium.getBasePrice().compareTo(new BigDecimal("100.25")));
    }

    @Test
    void heldAndBookedSeatsAreRestoredToTheSameStatus() {
        Stadium stadium = sampleStadium();
        Section lower = stadium.getStands().get(0).findSectionByCode("N1").orElseThrow();

        lower.findSeatById("N1-A-01").orElseThrow().hold();
        Seat booked = lower.findSeatById("N1-A-02").orElseThrow();
        booked.hold();
        booked.confirmBooking();

        Section restored = roundTrip(stadium).getStands().get(0)
                .findSectionByCode("N1").orElseThrow();

        assertEquals(SeatStatus.HELD,
                restored.findSeatById("N1-A-01").orElseThrow().getStatus());
        assertEquals(SeatStatus.BOOKED,
                restored.findSeatById("N1-A-02").orElseThrow().getStatus());
        assertEquals(SeatStatus.AVAILABLE,
                restored.findSeatById("N1-A-03").orElseThrow().getStatus());
    }

    @Test
    void availabilityCountsMatchAfterARoundTrip() {
        Stadium stadium = sampleStadium();
        long availableBefore = stadium.getAvailableSeatCount();

        Section lower = stadium.getStands().get(0).findSectionByCode("N1").orElseThrow();
        lower.findSeatById("N1-B-01").orElseThrow().hold();

        Stadium restored = roundTrip(stadium);
        assertEquals(availableBefore - 1, restored.getAvailableSeatCount());
    }

    @Test
    void everySeatRowHasExactlyOneParent() {
        StadiumRows rows = InventoryMapper.flatten(sampleStadium());

        assertEquals(18, rows.seats().size()); // 12 + 4 section seats, plus 2 flat seats
        for (SeatEntity seat : rows.seats()) {
            boolean inRow = seat.getRowId() != null;
            boolean onStand = seat.getStandId() != null;
            assertTrue(inRow ^ onStand,
                    "Seat " + seat.getId() + " must have exactly one parent");
        }
    }

    @Test
    void flattenedRowCountsLineUpWithTheDomain() {
        StadiumRows rows = InventoryMapper.flatten(sampleStadium());

        assertEquals(2, rows.stands().size());
        assertEquals(2, rows.sections().size());
        assertEquals(5, rows.rows().size());   // 3 rows in N1 plus 2 in N3
        assertEquals(18, rows.seats().size()); // 12 in N1, 4 in N3, 2 on the legacy stand
    }

    @Test
    void ordinalsRecordThePositionWithinTheParent() {
        StadiumRows rows = InventoryMapper.flatten(sampleStadium());

        assertEquals(List.of(0, 1), rows.stands().stream()
                .map(stand -> stand.getOrdinal()).toList());
        assertEquals(List.of(0, 1), rows.sections().stream()
                .map(section -> section.getOrdinal()).toList());
    }

    @Test
    void oneSectionCanBeFlattenedOnItsOwn() {
        Section lower = new Section("sec-x", "X1", "Test Section",
                SeatCategory.PREMIUM, new BigDecimal("200"));
        lower.generateRows(2, 3);

        SectionRows rows = InventoryMapper.flattenSection("stand-x", lower, 4);

        assertEquals("sec-x", rows.section().getId());
        assertEquals("stand-x", rows.section().getStandId());
        assertEquals("X1", rows.section().getCode());
        assertEquals("PREMIUM", rows.section().getCategory());
        assertEquals("200", rows.section().getBasePrice());
        assertEquals(4, rows.section().getOrdinal());
        assertEquals(2, rows.rows().size());
        assertEquals(6, rows.seats().size());
        assertTrue(rows.seats().stream().allMatch(seat -> seat.getStandId() == null));
        assertTrue(rows.seats().stream().allMatch(seat -> seat.getRowId() != null));
    }

    @Test
    void anEmptyStadiumRoundTripsToAnEmptyStadium() {
        Stadium empty = new Stadium("stadium-empty", "Empty Arena", "Dubai");
        Stadium restored = roundTrip(empty);

        assertEquals("stadium-empty", restored.getId());
        assertTrue(restored.getStands().isEmpty());
        assertEquals(0, restored.getAvailableSeatCount());
    }

    @Test
    void matchRoundTripsIncludingItsStatus() {
        Stadium stadium = sampleStadium();
        LocalDateTime kickoff = LocalDateTime.of(2026, 12, 24, 19, 30);

        Match scheduled = new Match("m-1", "Dubai Falcons", "Abu Dhabi United", kickoff, stadium);
        MatchEntity scheduledRow = InventoryMapper.toEntity(scheduled);
        assertEquals("stadium-1", scheduledRow.getStadiumId());
        assertEquals("2026-12-24T19:30", scheduledRow.getKickoff());
        Match restoredScheduled = InventoryMapper.toMatch(scheduledRow, stadium);
        assertEquals(MatchStatus.SCHEDULED, restoredScheduled.getStatus());
        assertEquals(kickoff, restoredScheduled.getKickoff());
        assertEquals("Dubai Falcons", restoredScheduled.getHomeTeam());
        assertEquals("Abu Dhabi United", restoredScheduled.getAwayTeam());
        assertNotNull(restoredScheduled.getStadium());

        Match onSale = new Match("m-2", "A", "B", kickoff, stadium);
        onSale.putOnSale();
        assertEquals(MatchStatus.ON_SALE,
                InventoryMapper.toMatch(InventoryMapper.toEntity(onSale), stadium).getStatus());

        Match cancelled = new Match("m-3", "A", "B", kickoff, stadium);
        cancelled.cancel();
        assertEquals(MatchStatus.CANCELLED,
                InventoryMapper.toMatch(InventoryMapper.toEntity(cancelled), stadium).getStatus());
    }

    @Test
    void unreachableMatchStatusesFallBackToScheduled() {
        // Match exposes no transition to SOLD_OUT or COMPLETED, so such a row cannot be
        // produced by the application today and cannot be replayed either. Documented
        // behaviour: it comes back as SCHEDULED rather than throwing.
        Stadium stadium = sampleStadium();
        MatchEntity soldOut = new MatchEntity(
                "m-4", "stadium-1", "A", "B", "2026-12-24T19:30", "SOLD_OUT");

        assertEquals(MatchStatus.SCHEDULED,
                InventoryMapper.toMatch(soldOut, stadium).getStatus());
    }
}
