package com.smartstadium.service;

import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.SeatCategory;
import com.smartstadium.domain.stadium.Section;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import com.smartstadium.persistence.InventoryStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Module 2: stadium structure and seat inventory.
 *
 * <p>This service owns the seating hierarchy
 * {@code Stadium -> Stand -> Section -> SeatRow -> Seat} and is the only place that
 * creates sections and generates their seats. Stadiums and stands themselves are not
 * stored here: they live in {@link AdminCatalogService}, which already holds them for
 * the admin API, so this service builds on that registry instead of keeping a second
 * copy of the same data.</p>
 *
 * <p>{@link #findSeat(String, String)} is the lookup the booking engine needs to resolve
 * a seat id coming from the browser into the live domain object it must hold or book. It
 * is deliberately the narrowest possible seam between the two modules.</p>
 *
 * <p>Module 6 made this durable. The live aggregate is still held in memory, because the
 * rest of the application passes {@link Seat} objects around and expects to see one
 * another's changes, but every change is written to the database as it happens and read
 * back on the next start. The database is what survives; memory is the single live copy.</p>
 *
 * <p>That is also why seat status has its own methods here ({@link #holdSeat},
 * {@link #confirmSeatBooking} and friends) rather than leaving callers to mutate a
 * {@link Seat} directly. Calling {@code findSeat(...).hold()} changes memory only; calling
 * {@code holdSeat(...)} changes memory and the database together. The booking engine
 * should use these.</p>
 */
@Service
public class SeatInventoryService {

    private final AdminCatalogService catalog;
    private final InventoryStore store;
    private final boolean seedDemoBowl;

    public SeatInventoryService(
            AdminCatalogService catalog,
            InventoryStore store,
            @Value("${smartstadium.inventory.seed-demo-bowl:true}") boolean seedDemoBowl) {
        this.catalog = catalog;
        this.store = store;
        this.seedDemoBowl = seedDemoBowl;
    }

    /**
     * Seeds the demo bowl, but only when no sectioned inventory exists yet.
     *
     * <p>{@link AdminCatalogService} has already restored anything saved by earlier runs
     * by the time this runs, so the presence of a single section anywhere means the
     * inventory came from the database and must not be seeded over.</p>
     */
    @PostConstruct
    void seedIfEmpty() {
        if (seedDemoBowl && !hasSectionedInventory()) {
            seedDemoBowl();
        }
    }

    private boolean hasSectionedInventory() {
        return catalog.getStadiums().stream()
                .flatMap(stadium -> stadium.getStands().stream())
                .anyMatch(stand -> !stand.getSections().isEmpty());
    }

    // --- queries --------------------------------------------------------------------

    public List<Stadium> getStadiums() {
        return catalog.getStadiums();
    }

    public Optional<Stadium> findStadium(String stadiumId) {
        if (stadiumId == null || stadiumId.isBlank()) {
            return Optional.empty();
        }
        String wanted = stadiumId.trim();
        return catalog.getStadiums().stream()
                .filter(stadium -> stadium.getId().equals(wanted))
                .findFirst();
    }

    public Optional<Stand> findStand(String stadiumId, String standId) {
        if (standId == null || standId.isBlank()) {
            return Optional.empty();
        }
        String wanted = standId.trim();
        return findStadium(stadiumId).stream()
                .flatMap(stadium -> stadium.getStands().stream())
                .filter(stand -> stand.getId().equals(wanted))
                .findFirst();
    }

    public Optional<Section> findSection(String stadiumId, String standId, String sectionId) {
        return findStand(stadiumId, standId).flatMap(stand -> stand.findSectionById(sectionId));
    }

    /**
     * Resolves a seat id within a stadium to the live {@link Seat}.
     *
     * <p>This is the entry point the booking engine should call before holding or
     * confirming seats, so that seat state has exactly one owner: the inventory.</p>
     */
    public Optional<Seat> findSeat(String stadiumId, String seatId) {
        if (seatId == null || seatId.isBlank()) {
            return Optional.empty();
        }
        String wanted = seatId.trim();
        return findStadium(stadiumId).stream()
                .flatMap(stadium -> stadium.getStands().stream())
                .flatMap(stand -> stand.getAllSeats().stream())
                .filter(seat -> seat.getId().equals(wanted))
                .findFirst();
    }

    /** Resolves several seat ids at once, preserving the order asked for. */
    public List<Seat> findSeats(String stadiumId, List<String> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }
        return seatIds.stream()
                .map(seatId -> findSeat(stadiumId, seatId))
                .flatMap(Optional::stream)
                .toList();
    }

    public int capacityOf(String stadiumId) {
        return findStadium(stadiumId)
                .map(stadium -> stadium.getStands().stream().mapToInt(Stand::getCapacity).sum())
                .orElse(0);
    }

    public long availableSeatsOf(String stadiumId) {
        return findStadium(stadiumId).map(Stadium::getAvailableSeatCount).orElse(0L);
    }

    // --- inventory management -------------------------------------------------------

    /**
     * Creates a section in a stand and generates its rows and seats in one step.
     *
     * <p>Section codes must be unique across the whole stadium, not just within the
     * stand, because seat labels are built from the section code and the seat map relies
     * on those labels being unambiguous.</p>
     *
     * @throws IllegalArgumentException if the stadium or stand is unknown, if the section
     *                                  code is already used anywhere in the stadium, or if
     *                                  the row and seat counts are out of range
     */
    public synchronized Section createSection(String stadiumId, String standId, String code,
                                              String name, SeatCategory category,
                                              BigDecimal basePrice, int rowCount, int seatsPerRow) {
        Stadium stadium = findStadium(stadiumId)
                .orElseThrow(() -> new IllegalArgumentException("Stadium not found."));
        Stand stand = stadium.getStands().stream()
                .filter(candidate -> candidate.getId().equals(standId == null ? "" : standId.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Stand not found in this stadium."));

        requireSectionCodeFreeInStadium(stadium, code);

        Section section = new Section(UUID.randomUUID().toString(), code, name, category, basePrice);
        section.generateRows(rowCount, seatsPerRow);
        stand.addSection(section);
        store.persistSection(stand.getId(), section, stand.getSections().size() - 1);
        return section;
    }

    // --- seat status, memory and database together -----------------------------------

    /**
     * Holds a seat and saves the new status.
     *
     * @throws IllegalArgumentException if the seat does not exist in that stadium
     * @throws IllegalStateException    if the seat is not available, from {@link Seat#hold()}
     */
    public synchronized Seat holdSeat(String stadiumId, String seatId) {
        return changeSeat(stadiumId, seatId, Seat::hold);
    }

    /** Releases a held seat and saves the new status. */
    public synchronized Seat releaseSeatHold(String stadiumId, String seatId) {
        return changeSeat(stadiumId, seatId, Seat::releaseHold);
    }

    /** Turns a held seat into a booked one and saves the new status. */
    public synchronized Seat confirmSeatBooking(String stadiumId, String seatId) {
        return changeSeat(stadiumId, seatId, Seat::confirmBooking);
    }

    /** Frees a booked seat and saves the new status. */
    public synchronized Seat cancelSeatBooking(String stadiumId, String seatId) {
        return changeSeat(stadiumId, seatId, Seat::cancelBooking);
    }

    /**
     * Applies a status transition and writes it. The domain object decides whether the
     * transition is legal, so an invalid one throws before anything is saved.
     */
    private Seat changeSeat(String stadiumId, String seatId, java.util.function.Consumer<Seat> change) {
        Seat seat = findSeat(stadiumId, seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
        change.accept(seat);
        store.updateSeatStatus(seat);
        return seat;
    }

    private void requireSectionCodeFreeInStadium(Stadium stadium, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Section code is required.");
        }
        String wanted = code.trim();
        boolean taken = stadium.getStands().stream()
                .anyMatch(stand -> stand.findSectionByCode(wanted).isPresent());
        if (taken) {
            throw new IllegalArgumentException(
                    "Section code already used in this stadium: " + wanted.toUpperCase());
        }
    }

    // --- demo bowl ------------------------------------------------------------------

    /**
     * Builds one fully seated stadium so the seat map and the booking flow have realistic
     * inventory to work against before the database module lands.
     *
     * <p>It is registered through {@link AdminCatalogService}, so it shows up in the admin
     * APIs alongside anything an administrator creates by hand. The original demo fixture
     * in {@link DemoDataService} is left exactly as it is.</p>
     *
     * <p>Set {@code smartstadium.inventory.seed-demo-bowl=false} to start with no seeded
     * inventory at all.</p>
     */
    private void seedDemoBowl() {
        Stadium stadium = catalog.createStadium("Dubai Grand Arena", "Dubai");

        for (StandBlueprint blueprint : BOWL) {
            Stand stand = catalog.addStand(stadium.getId(), blueprint.standName());
            for (SectionBlueprint section : blueprint.sections()) {
                createSection(
                        stadium.getId(),
                        stand.getId(),
                        section.code(),
                        section.name(),
                        section.category(),
                        section.basePrice(),
                        section.rowCount(),
                        section.seatsPerRow());
            }
        }
    }

    private record SectionBlueprint(String code, String name, SeatCategory category,
                                    BigDecimal basePrice, int rowCount, int seatsPerRow) {}

    private record StandBlueprint(String standName, List<SectionBlueprint> sections) {}

    private static final List<StandBlueprint> BOWL = List.of(
            new StandBlueprint("North Stand", List.of(
                    new SectionBlueprint("N1", "North Lower Tier", SeatCategory.REGULAR,
                            new BigDecimal("150"), 12, 24),
                    new SectionBlueprint("N2", "North Upper Tier", SeatCategory.PREMIUM,
                            new BigDecimal("320"), 8, 24),
                    new SectionBlueprint("N3", "North Executive Box", SeatCategory.VIP,
                            new BigDecimal("600"), 4, 16))),
            new StandBlueprint("South Stand", List.of(
                    new SectionBlueprint("S1", "South Lower Tier", SeatCategory.REGULAR,
                            new BigDecimal("150"), 12, 24),
                    new SectionBlueprint("S2", "South Upper Tier", SeatCategory.PREMIUM,
                            new BigDecimal("320"), 8, 24),
                    new SectionBlueprint("S3", "South Family Enclosure", SeatCategory.REGULAR,
                            new BigDecimal("180"), 4, 16))),
            new StandBlueprint("East Stand", List.of(
                    new SectionBlueprint("E1", "East Lower Tier", SeatCategory.REGULAR,
                            new BigDecimal("200"), 10, 28),
                    new SectionBlueprint("E2", "East Club Seats", SeatCategory.PREMIUM,
                            new BigDecimal("400"), 6, 20),
                    new SectionBlueprint("E3", "East Hospitality", SeatCategory.VIP,
                            new BigDecimal("750"), 3, 14))),
            new StandBlueprint("West Stand", List.of(
                    new SectionBlueprint("W1", "West Main Tier", SeatCategory.PREMIUM,
                            new BigDecimal("450"), 10, 28),
                    new SectionBlueprint("W2", "West Directors Box", SeatCategory.VIP,
                            new BigDecimal("900"), 4, 12),
                    new SectionBlueprint("W3", "West Media Tier", SeatCategory.REGULAR,
                            new BigDecimal("220"), 6, 20))));
}
