package com.smartstadium.persistence;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.stadium.Section;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import com.smartstadium.persistence.InventoryMapper.StadiumRows;
import com.smartstadium.persistence.entity.MatchEntity;
import com.smartstadium.persistence.entity.SeatEntity;
import com.smartstadium.persistence.entity.SeatRowEntity;
import com.smartstadium.persistence.entity.SectionEntity;
import com.smartstadium.persistence.entity.StadiumEntity;
import com.smartstadium.persistence.entity.StandEntity;
import com.smartstadium.persistence.repository.MatchRepository;
import com.smartstadium.persistence.repository.SeatRepository;
import com.smartstadium.persistence.repository.SeatRowRepository;
import com.smartstadium.persistence.repository.SectionRepository;
import com.smartstadium.persistence.repository.StadiumRepository;
import com.smartstadium.persistence.repository.StandRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The single door between the services and the database.
 *
 * <p>Services depend on this class rather than on the repositories, so the rest of the
 * application never deals in entities. Everything crossing this boundary is a domain
 * object.</p>
 *
 * <p>Reads load the whole inventory in six queries, no matter how many stadiums there
 * are, and assemble the trees in Java. That avoids lazy loading and fetch joins
 * entirely, which is also why the entities carry plain id columns instead of
 * {@code @OneToMany} associations.</p>
 *
 * <p>Structural writes go through {@link #persistStadium(Stadium)}, which upserts the
 * whole aggregate. It is idempotent, and structural changes are rare: seeding once at
 * first start, and an administrator adding a stand or section. Seat status changes are
 * frequent, so they have their own single-row path in
 * {@link #updateSeatStatus(Seat)}.</p>
 *
 * <p>Note that {@code persistStadium} inserts and updates but never deletes. Nothing in
 * the application removes a stand, section or seat yet; when something does, it will need
 * an explicit delete here.</p>
 */
@Component
public class InventoryStore {

    private final StadiumRepository stadiums;
    private final StandRepository stands;
    private final SectionRepository sections;
    private final SeatRowRepository seatRows;
    private final SeatRepository seats;
    private final MatchRepository matches;

    public InventoryStore(StadiumRepository stadiums, StandRepository stands,
                          SectionRepository sections, SeatRowRepository seatRows,
                          SeatRepository seats, MatchRepository matches) {
        this.stadiums = stadiums;
        this.stands = stands;
        this.sections = sections;
        this.seatRows = seatRows;
        this.seats = seats;
        this.matches = matches;
    }

    // --- reads ----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public boolean hasStadiums() {
        return stadiums.count() > 0;
    }

    /** Loads every stadium with all of its stands, sections, rows and seats. */
    @Transactional(readOnly = true)
    public List<Stadium> loadStadiums() {
        List<StadiumEntity> stadiumRows = stadiums.findAll();
        if (stadiumRows.isEmpty()) {
            return List.of();
        }

        List<StandEntity> standRows = stands.findAll();
        List<SectionEntity> sectionRows = sections.findAll();
        List<SeatRowEntity> rowRows = seatRows.findAll();
        List<SeatEntity> seatRowsAll = seats.findAll();

        Map<String, List<StandEntity>> standsByStadium = standRows.stream()
                .collect(Collectors.groupingBy(StandEntity::getStadiumId));
        Map<String, List<SectionEntity>> sectionsByStand = sectionRows.stream()
                .collect(Collectors.groupingBy(SectionEntity::getStandId));
        Map<String, List<SeatRowEntity>> rowsBySection = rowRows.stream()
                .collect(Collectors.groupingBy(SeatRowEntity::getSectionId));

        List<Stadium> loaded = new ArrayList<>(stadiumRows.size());
        for (StadiumEntity stadiumRow : stadiumRows) {
            List<StandEntity> ownStands =
                    standsByStadium.getOrDefault(stadiumRow.getId(), List.of());
            Set<String> standIds = ownStands.stream()
                    .map(StandEntity::getId).collect(Collectors.toSet());

            List<SectionEntity> ownSections = standIds.stream()
                    .flatMap(standId -> sectionsByStand.getOrDefault(standId, List.of()).stream())
                    .toList();
            Set<String> sectionIds = ownSections.stream()
                    .map(SectionEntity::getId).collect(Collectors.toSet());

            List<SeatRowEntity> ownRows = sectionIds.stream()
                    .flatMap(sectionId -> rowsBySection.getOrDefault(sectionId, List.of()).stream())
                    .toList();
            Set<String> rowIds = ownRows.stream()
                    .map(SeatRowEntity::getId).collect(Collectors.toSet());

            List<SeatEntity> ownSeats = seatRowsAll.stream()
                    .filter(seat -> (seat.getRowId() != null && rowIds.contains(seat.getRowId()))
                            || (seat.getStandId() != null && standIds.contains(seat.getStandId())))
                    .toList();

            loaded.add(InventoryMapper.assemble(new StadiumRows(
                    stadiumRow, ownStands, ownSections, ownRows, ownSeats)));
        }
        return loaded;
    }

    /**
     * Loads every match, attaching each one to the stadium it belongs to.
     *
     * <p>A match whose stadium is missing is skipped rather than guessed at. The schema's
     * foreign key makes that impossible in practice.</p>
     */
    @Transactional(readOnly = true)
    public List<Match> loadMatches(List<Stadium> knownStadiums) {
        Map<String, Stadium> byId = new HashMap<>();
        knownStadiums.forEach(stadium -> byId.put(stadium.getId(), stadium));

        List<Match> loaded = new ArrayList<>();
        for (MatchEntity row : matches.findAll()) {
            Stadium stadium = byId.get(row.getStadiumId());
            if (stadium != null) {
                loaded.add(InventoryMapper.toMatch(row, stadium));
            }
        }
        return loaded;
    }

    // --- writes ---------------------------------------------------------------------

    /**
     * Upserts a stadium and everything inside it. Safe to call repeatedly: rows are
     * written by their own ids, so a second call updates rather than duplicates.
     *
     * <p>This writes every seat, so it is used for the one-off seed and not for routine
     * changes. Adding a stand or a section has its own narrower method, because ids are
     * assigned by the application rather than generated by the database, which makes
     * every {@code save} a merge with a SELECT in front of it. Re-saving 2,000 seats to
     * add one section would be thousands of needless statements.</p>
     */
    @Transactional
    public void persistStadium(Stadium stadium) {
        StadiumRows rows = InventoryMapper.flatten(stadium);
        stadiums.save(rows.stadium());
        stands.saveAll(rows.stands());
        sections.saveAll(rows.sections());
        seatRows.saveAll(rows.rows());
        seats.saveAll(rows.seats());
    }

    /** Writes just the stadium's own row, without its stands or seats. */
    @Transactional
    public void persistStadiumRow(Stadium stadium) {
        stadiums.save(InventoryMapper.toEntity(stadium));
    }

    /**
     * Writes one stand and whatever it currently holds. A stand is empty when an
     * administrator first adds it, so this is normally a single row.
     */
    @Transactional
    public void persistStand(String stadiumId, Stand stand, int ordinal) {
        InventoryMapper.StandRows rows =
                InventoryMapper.flattenStand(stadiumId, stand, ordinal);
        stands.save(rows.stand());
        sections.saveAll(rows.sections());
        seatRows.saveAll(rows.rows());
        seats.saveAll(rows.seats());
    }

    /** Writes one newly created section, its rows and its seats, and nothing else. */
    @Transactional
    public void persistSection(String standId, Section section, int ordinal) {
        InventoryMapper.SectionRows rows =
                InventoryMapper.flattenSection(standId, section, ordinal);
        sections.save(rows.section());
        seatRows.saveAll(rows.rows());
        seats.saveAll(rows.seats());
    }

    /**
     * Writes one seat's status. This is the hot path: it runs whenever a seat is held,
     * booked, released or cancelled, so it touches a single row.
     */
    @Transactional
    public void updateSeatStatus(Seat seat) {
        Optional<SeatEntity> existing = seats.findById(seat.getId());
        if (existing.isEmpty()) {
            return;
        }
        SeatEntity row = existing.get();
        row.setStatus(seat.getStatus().name());
        seats.save(row);
    }

    /**
     * Saves a match, making sure its stadium row exists first.
     *
     * <p>A match can point at the in-memory demo fixture, which is never persisted as a
     * full aggregate because it is rebuilt on every start. Without this upsert the
     * {@code football_match.stadium_id} foreign key would reject such a match. Only the
     * stadium row is written here, not its stands and seats.</p>
     */
    @Transactional
    public void persistMatch(Match match) {
        Stadium stadium = match.getStadium();
        stadiums.save(new StadiumEntity(stadium.getId(), stadium.getName(), stadium.getCity()));
        matches.save(InventoryMapper.toEntity(match));
    }
}
