package com.smartstadium.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Database row for a stadium.
 *
 * <p>The domain class {@code Stadium} is deliberately not an entity. Its fields are
 * {@code final} and it has no no-argument constructor, which Hibernate requires, so
 * annotating it would mean loosening its encapsulation. This class carries the row and
 * {@code InventoryMapper} converts between the two.</p>
 *
 * <p>Parent links are plain id columns rather than {@code @ManyToOne} associations. The
 * whole stadium aggregate is read once at startup and assembled in Java, so there is
 * nothing for lazy loading or cascades to do, and the foreign keys in the schema still
 * enforce the relationships.</p>
 */
@Entity
@Table(name = "stadium")
public class StadiumEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "city", nullable = false)
    private String city;

    protected StadiumEntity() {
        // Required by Hibernate.
    }

    public StadiumEntity(String id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}
