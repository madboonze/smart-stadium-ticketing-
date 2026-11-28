package com.smartstadium.domain.stadium;

import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.SeatCategory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A seating section inside a {@link Stand}, for example the lower tier behind the north goal.
 *
 * <p>A section is the unit a customer picks before zooming in to individual seats, so it
 * carries the two things that decide what those seats cost: a {@link SeatCategory} and a
 * base price. Every seat generated for the section is created through {@link SeatFactory}
 * with that category and price, which means the existing pricing rules on
 * {@code RegularSeat}, {@code PremiumSeat} and {@code VIPSeat} apply unchanged.</p>
 *
 * <p>Seat labels follow {@code CODE-ROW-NN}, for example {@code N1-A-07}. Section codes are
 * expected to be unique within a stadium, which makes every seat label unique stadium-wide
 * and gives the 3D seat map a readable identifier to work with.</p>
 */
public class Section {
    private static final int MAX_SEATS_PER_ROW = 200;
    private static final int MAX_ROWS = 200;

    private final String id;
    private final String code;
    private final String name;
    private final SeatCategory category;
    private final BigDecimal basePrice;
    private final List<SeatRow> rows = new ArrayList<>();

    /**
     * @param id        stable identifier, unique within the owning stand
     * @param code      short code used in seat labels, such as {@code "N1"}
     * @param name      display name, such as {@code "North Lower Tier"}
     * @param category  seat category applied to every seat in this section
     * @param basePrice base price per seat before the category multiplier, must be positive
     */
    public Section(String id, String code, String name, SeatCategory category, BigDecimal basePrice) {
        if (id == null || id.isBlank() || code == null || code.isBlank()
                || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Section id, code and name are required.");
        }
        Objects.requireNonNull(category, "Section seat category is required");
        Objects.requireNonNull(basePrice, "Section base price is required");
        if (basePrice.signum() <= 0) {
            throw new IllegalArgumentException("Section base price must be greater than zero.");
        }
        this.id = id.trim();
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.category = category;
        this.basePrice = basePrice;
    }

    /**
     * Adds an already built row to this section.
     *
     * @throws IllegalArgumentException if a row with the same id or label is already present
     */
    public void addRow(SeatRow row) {
        Objects.requireNonNull(row, "Seat row cannot be null");
        boolean duplicate = rows.stream().anyMatch(existing ->
                existing.getId().equals(row.getId())
                        || existing.getLabel().equalsIgnoreCase(row.getLabel()));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate row id or label in section " + code);
        }
        rows.add(row);
    }

    /**
     * Builds {@code rowCount} rows of {@code seatsPerRow} seats each and adds them to this
     * section. Rows are labelled A, B, C ... and seats are numbered from 1 within each row.
     *
     * <p>This is the normal way to populate a section: one call creates the rows, creates the
     * seats through {@link SeatFactory} using this section's category and base price, and
     * wires them together.</p>
     *
     * @return the rows that were created, in order
     * @throws IllegalArgumentException if the counts are outside a sensible range, or if
     *                                  generating would collide with rows already present
     */
    public List<SeatRow> generateRows(int rowCount, int seatsPerRow) {
        if (rowCount <= 0 || rowCount > MAX_ROWS) {
            throw new IllegalArgumentException("Row count must be between 1 and " + MAX_ROWS + ".");
        }
        if (seatsPerRow <= 0 || seatsPerRow > MAX_SEATS_PER_ROW) {
            throw new IllegalArgumentException(
                    "Seats per row must be between 1 and " + MAX_SEATS_PER_ROW + ".");
        }

        int offset = rows.size();
        List<SeatRow> created = new ArrayList<>(rowCount);
        for (int r = 0; r < rowCount; r++) {
            String rowLabel = rowLabel(offset + r);
            SeatRow row = new SeatRow(id + "-" + rowLabel, rowLabel);
            for (int s = 1; s <= seatsPerRow; s++) {
                String seatLabel = "%s-%s-%02d".formatted(code, rowLabel, s);
                row.addSeat(SeatFactory.create(category, seatLabel, seatLabel, basePrice));
            }
            addRow(row);
            created.add(row);
        }
        return Collections.unmodifiableList(created);
    }

    /**
     * Spreadsheet style row label for a zero based index: 0 is A, 25 is Z, 26 is AA.
     */
    public static String rowLabel(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Row index cannot be negative.");
        }
        StringBuilder label = new StringBuilder();
        int remaining = index;
        while (true) {
            label.insert(0, (char) ('A' + (remaining % 26)));
            remaining = remaining / 26 - 1;
            if (remaining < 0) {
                break;
            }
        }
        return label.toString();
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public SeatCategory getCategory() {
        return category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public List<SeatRow> getRows() {
        return Collections.unmodifiableList(rows);
    }

    /** Every seat in this section, flattened across its rows. */
    public List<Seat> getSeats() {
        return rows.stream().flatMap(row -> row.getSeats().stream()).toList();
    }

    /** Total number of seats in this section, whatever their status. */
    public int getCapacity() {
        return rows.stream().mapToInt(SeatRow::getCapacity).sum();
    }

    public long getAvailableSeatCount() {
        return rows.stream().mapToLong(SeatRow::getAvailableSeatCount).sum();
    }

    public Optional<SeatRow> findRowByLabel(String rowLabel) {
        if (rowLabel == null || rowLabel.isBlank()) {
            return Optional.empty();
        }
        return rows.stream()
                .filter(row -> row.getLabel().equalsIgnoreCase(rowLabel.trim()))
                .findFirst();
    }

    public Optional<Seat> findSeatById(String seatId) {
        if (seatId == null || seatId.isBlank()) {
            return Optional.empty();
        }
        return rows.stream()
                .map(row -> row.findSeatById(seatId))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
