package com.smartstadium.domain.stadium;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Stadium {
    private final String id;
    private final String name;
    private final String city;
    private final List<Stand> stands = new ArrayList<>();

    public Stadium(String id, String name, String city) {
        if (id == null || id.isBlank() || name == null || name.isBlank()
                || city == null || city.isBlank()) {
            throw new IllegalArgumentException("Stadium id, name and city are required.");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.city = city.trim();
    }

    public void addStand(Stand stand) {
        Objects.requireNonNull(stand, "Stand cannot be null");
        if (stands.stream().anyMatch(s -> s.getId().equals(stand.getId()))) {
            throw new IllegalArgumentException("Stand already exists: " + stand.getId());
        }
        stands.add(stand);
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

    public List<Stand> getStands() {
        return Collections.unmodifiableList(stands);
    }

    public long getAvailableSeatCount() {
        return stands.stream().mapToLong(Stand::getAvailableSeatCount).sum();
    }
}
