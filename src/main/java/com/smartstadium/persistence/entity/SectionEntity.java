package com.smartstadium.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Database row for a seating section.
 *
 * <p>{@code basePrice} is stored as the exact {@code BigDecimal} string rather than a
 * numeric column. SQLite's dynamic typing can turn a numeric money column into a
 * floating point value, which would quietly change prices.</p>
 */
@Entity
@Table(name = "section")
public class SectionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "stand_id", nullable = false)
    private String standId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    /** {@code SeatCategory} name. */
    @Column(name = "category", nullable = false)
    private String category;

    /** Exact decimal string, for example {@code "320"} or {@code "99.50"}. */
    @Column(name = "base_price", nullable = false)
    private String basePrice;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    protected SectionEntity() {
        // Required by Hibernate.
    }

    public SectionEntity(String id, String standId, String code, String name,
                         String category, String basePrice, int ordinal) {
        this.id = id;
        this.standId = standId;
        this.code = code;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.ordinal = ordinal;
    }

    public String getId() {
        return id;
    }

    public String getStandId() {
        return standId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getBasePrice() {
        return basePrice;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
