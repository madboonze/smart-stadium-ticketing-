package com.smartstadium.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Database row for a stand. {@code ordinal} preserves the order stands were added in. */
@Entity
@Table(name = "stand")
public class StandEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "stadium_id", nullable = false)
    private String stadiumId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    protected StandEntity() {
        // Required by Hibernate.
    }

    public StandEntity(String id, String stadiumId, String name, int ordinal) {
        this.id = id;
        this.stadiumId = stadiumId;
        this.name = name;
        this.ordinal = ordinal;
    }

    public String getId() {
        return id;
    }

    public String getStadiumId() {
        return stadiumId;
    }

    public String getName() {
        return name;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
