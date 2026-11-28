package com.smartstadium.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Database row for one row of seats inside a section. */
@Entity
@Table(name = "seat_row")
public class SeatRowEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "section_id", nullable = false)
    private String sectionId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    protected SeatRowEntity() {
        // Required by Hibernate.
    }

    public SeatRowEntity(String id, String sectionId, String label, int ordinal) {
        this.id = id;
        this.sectionId = sectionId;
        this.label = label;
        this.ordinal = ordinal;
    }

    public String getId() {
        return id;
    }

    public String getSectionId() {
        return sectionId;
    }

    public String getLabel() {
        return label;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
