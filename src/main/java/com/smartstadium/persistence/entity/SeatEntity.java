package com.smartstadium.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Database row for a single seat, and the one row in this schema that changes during
 * normal use: {@code status} moves between AVAILABLE, HELD and BOOKED as customers book.
 *
 * <p>A seat has exactly one parent. {@code rowId} is set when the seat belongs to a
 * section, {@code standId} when it was attached straight to a stand with
 * {@code Stand.addSeat}. The schema has a CHECK constraint enforcing that exactly one of
 * the two is present.</p>
 */
@Entity
@Table(name = "seat")
public class SeatEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    /** Set when this seat sits in a section row; null when it hangs off a stand. */
    @Column(name = "row_id")
    private String rowId;

    /** Set when this seat was attached straight to a stand; null when it sits in a row. */
    @Column(name = "stand_id")
    private String standId;

    @Column(name = "label", nullable = false)
    private String label;

    /** {@code SeatCategory} name, which decides the seat subclass on load. */
    @Column(name = "category", nullable = false)
    private String category;

    /** Exact decimal string of the base price, before the category multiplier. */
    @Column(name = "base_price", nullable = false)
    private String basePrice;

    /** {@code SeatStatus} name. */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    protected SeatEntity() {
        // Required by Hibernate.
    }

    public SeatEntity(String id, String rowId, String standId, String label,
                      String category, String basePrice, String status, int ordinal) {
        this.id = id;
        this.rowId = rowId;
        this.standId = standId;
        this.label = label;
        this.category = category;
        this.basePrice = basePrice;
        this.status = status;
        this.ordinal = ordinal;
    }

    public String getId() {
        return id;
    }

    public String getRowId() {
        return rowId;
    }

    public String getStandId() {
        return standId;
    }

    public String getLabel() {
        return label;
    }

    public String getCategory() {
        return category;
    }

    public String getBasePrice() {
        return basePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
