package com.smartstadium.domain.stadium;

import com.smartstadium.domain.seat.PremiumSeat;
import com.smartstadium.domain.seat.RegularSeat;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.SeatCategory;
import com.smartstadium.domain.seat.VIPSeat;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module 2: the seating hierarchy Stadium -> Stand -> Section -> SeatRow -> Seat.
 */
class SeatInventoryTest {

    private static Section section(String code, SeatCategory category, String basePrice) {
        return new Section("sec-" + code, code, "Section " + code, category, new BigDecimal(basePrice));
    }

    @Test
    void rowLabelsFollowSpreadsheetOrder() {
        assertEquals("A", Section.rowLabel(0));
        assertEquals("Z", Section.rowLabel(25));
        assertEquals("AA", Section.rowLabel(26));
        assertEquals("AB", Section.rowLabel(27));
        assertThrows(IllegalArgumentException.class, () -> Section.rowLabel(-1));
    }

    @Test
    void generatingRowsBuildsEveryIndividualSeat() {
        Section lower = section("N1", SeatCategory.REGULAR, "150");
        List<SeatRow> rows = lower.generateRows(12, 24);

        assertEquals(12, rows.size());
        assertEquals(288, lower.getCapacity());
        assertEquals(288, lower.getAvailableSeatCount());
        assertEquals(288, lower.getSeats().size());
        assertEquals(24, rows.get(0).getCapacity());
    }

    @Test
    void seatLabelsIdentifySectionRowAndNumber() {
        Section lower = section("N1", SeatCategory.REGULAR, "150");
        lower.generateRows(2, 10);

        assertEquals("N1-A-01", lower.getSeats().get(0).getLabel());
        assertEquals("N1-A-10", lower.getSeats().get(9).getLabel());
        assertEquals("N1-B-01", lower.getSeats().get(10).getLabel());
    }

    @Test
    void everySeatInAStandHasAUniqueLabel() {
        Stand north = new Stand("stand-n", "North Stand");
        north.addSection(withRows(section("N1", SeatCategory.REGULAR, "150"), 12, 24));
        north.addSection(withRows(section("N2", SeatCategory.PREMIUM, "320"), 8, 24));
        north.addSection(withRows(section("N3", SeatCategory.VIP, "600"), 4, 16));

        Set<String> labels = new HashSet<>();
        for (Seat seat : north.getAllSeats()) {
            assertTrue(labels.add(seat.getLabel()), "Duplicate seat label: " + seat.getLabel());
        }
        assertEquals(288 + 192 + 64, labels.size());
    }

    @Test
    void sectionCategoryDecidesSeatTypeAndPrice() {
        Seat regular = withRows(section("R1", SeatCategory.REGULAR, "150"), 1, 1).getSeats().get(0);
        Seat premium = withRows(section("P1", SeatCategory.PREMIUM, "300"), 1, 1).getSeats().get(0);
        Seat vip = withRows(section("V1", SeatCategory.VIP, "500"), 1, 1).getSeats().get(0);

        assertInstanceOf(RegularSeat.class, regular);
        assertInstanceOf(PremiumSeat.class, premium);
        assertInstanceOf(VIPSeat.class, vip);

        assertEquals(0, regular.calculatePrice().compareTo(new BigDecimal("150")));
        assertEquals(0, premium.calculatePrice().compareTo(new BigDecimal("450.00")));
        assertEquals(0, vip.calculatePrice().compareTo(new BigDecimal("1250.00")));
    }

    @Test
    void standRollsUpCapacityAndAvailabilityWithoutDoubleCounting() {
        Stand stand = new Stand("stand-e", "East Stand");
        stand.addSection(withRows(section("E1", SeatCategory.REGULAR, "200"), 10, 28));

        assertEquals(280, stand.getCapacity());
        assertEquals(280, stand.getAvailableSeatCount());
        assertEquals(280, stand.getAllSeats().size());
        assertTrue(stand.getSeats().isEmpty(), "Section seats must not leak into the flat seat list.");
    }

    @Test
    void seatsAttachedDirectlyToAStandKeepWorkingAlongsideSections() {
        Stand stand = new Stand("stand-mixed", "Mixed Stand");
        stand.addSeat(new RegularSeat("L-1", "L-1", new BigDecimal("100")));
        stand.addSeat(new PremiumSeat("L-2", "L-2", new BigDecimal("100")));
        stand.addSection(withRows(section("M1", SeatCategory.REGULAR, "120"), 2, 5));

        assertEquals(2, stand.getSeats().size());
        assertEquals(12, stand.getCapacity());
        assertEquals(12, stand.getAllSeats().size());
        assertEquals(12, stand.getAvailableSeatCount());
    }

    @Test
    void holdingASeatIsVisibleThroughTheWholeHierarchy() {
        Section lower = withRows(section("N1", SeatCategory.REGULAR, "150"), 2, 10);
        Stand stand = new Stand("stand-n", "North Stand");
        stand.addSection(lower);
        Stadium stadium = new Stadium("stadium-1", "Dubai Grand Arena", "Dubai");
        stadium.addStand(stand);

        Seat seat = lower.findSeatById("N1-A-01").orElseThrow();
        seat.hold();

        assertEquals(19, lower.getAvailableSeatCount());
        assertEquals(19, stand.getAvailableSeatCount());
        assertEquals(19, stadium.getAvailableSeatCount());
        assertEquals(20, stand.getCapacity());

        seat.confirmBooking();
        assertEquals(19, stadium.getAvailableSeatCount());

        seat.cancelBooking();
        assertEquals(20, stadium.getAvailableSeatCount());
    }

    @Test
    void sectionsAndRowsCanBeLookedUp() {
        Section lower = withRows(section("N1", SeatCategory.REGULAR, "150"), 3, 10);
        Stand stand = new Stand("stand-n", "North Stand");
        stand.addSection(lower);

        assertTrue(stand.findSectionById("sec-N1").isPresent());
        assertTrue(stand.findSectionByCode("n1").isPresent());
        assertTrue(stand.findSectionByCode("ZZ").isEmpty());
        assertTrue(lower.findRowByLabel("b").isPresent());
        assertTrue(lower.findSeatById("N1-C-07").isPresent());
        assertTrue(lower.findSeatById("N1-Z-99").isEmpty());
        assertFalse(lower.findSeatById(null).isPresent());
    }

    @Test
    void duplicateSectionCodesAndRowLabelsAreRejected() {
        Stand stand = new Stand("stand-n", "North Stand");
        stand.addSection(section("N1", SeatCategory.REGULAR, "150"));
        assertThrows(IllegalArgumentException.class,
                () -> stand.addSection(section("n1", SeatCategory.VIP, "600")));

        Section lower = section("N2", SeatCategory.REGULAR, "150");
        lower.addRow(new SeatRow("r-a", "A"));
        assertThrows(IllegalArgumentException.class, () -> lower.addRow(new SeatRow("r-a2", "a")));
    }

    @Test
    void invalidSectionDefinitionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Section("id", "  ", "Blank code", SeatCategory.REGULAR, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> new Section("id", "N1", "Zero price", SeatCategory.REGULAR, BigDecimal.ZERO));
        assertThrows(NullPointerException.class,
                () -> new Section("id", "N1", "No category", null, BigDecimal.TEN));

        Section lower = section("N1", SeatCategory.REGULAR, "150");
        assertThrows(IllegalArgumentException.class, () -> lower.generateRows(0, 10));
        assertThrows(IllegalArgumentException.class, () -> lower.generateRows(10, 0));
        assertThrows(IllegalArgumentException.class, () -> lower.generateRows(10, 5000));
    }

    @Test
    void generatingTwiceContinuesTheRowLabelling() {
        Section lower = section("N1", SeatCategory.REGULAR, "150");
        lower.generateRows(2, 3);
        lower.generateRows(2, 3);

        assertEquals(List.of("A", "B", "C", "D"),
                lower.getRows().stream().map(SeatRow::getLabel).toList());
        assertEquals(12, lower.getCapacity());
    }

    @Test
    void seatFactoryBuildsTheMatchingSubclassForEachCategory() {
        BigDecimal price = new BigDecimal("100");
        assertInstanceOf(RegularSeat.class,
                SeatFactory.create(SeatCategory.REGULAR, "a", "a", price));
        assertInstanceOf(PremiumSeat.class,
                SeatFactory.create(SeatCategory.PREMIUM, "b", "b", price));
        assertInstanceOf(VIPSeat.class,
                SeatFactory.create(SeatCategory.VIP, "c", "c", price));
        assertThrows(NullPointerException.class,
                () -> SeatFactory.create(null, "d", "d", price));
    }

    private static Section withRows(Section section, int rowCount, int seatsPerRow) {
        section.generateRows(rowCount, seatsPerRow);
        return section;
    }
}
