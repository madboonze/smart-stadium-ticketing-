package com.smartstadium.service;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import com.smartstadium.persistence.InventoryStore;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminCatalogService {
    private final Map<String, Stadium> stadiums = new ConcurrentHashMap<>();
    private final Map<String, Match> matches = new ConcurrentHashMap<>();
    private final InventoryStore store;

    public AdminCatalogService(DemoDataService demoData, InventoryStore store) {
        this.store = store;
        Stadium demo = demoData.getStadium();
        stadiums.put(demo.getId(), demo);
        matches.put(demoData.getMatch().getId(), demoData.getMatch());
    }

    /**
     * Module 6: brings back the stadiums and matches saved by earlier runs.
     *
     * <p>Runs after construction so the database is fully initialised, and before any
     * bean that depends on this one is created. {@code putIfAbsent} means the in-memory
     * demo fixture seeded in the constructor keeps its place: it is rebuilt identically
     * on every start, so it is never persisted and never needs restoring.</p>
     */
    @PostConstruct
    void restorePersistedCatalog() {
        store.loadStadiums().forEach(stadium -> stadiums.putIfAbsent(stadium.getId(), stadium));
        store.loadMatches(new ArrayList<>(stadiums.values()))
                .forEach(match -> matches.putIfAbsent(match.getId(), match));
    }

    public synchronized Stadium createStadium(String name, String city) {
        Stadium stadium = new Stadium(UUID.randomUUID().toString(), name, city);
        stadiums.put(stadium.getId(), stadium);
        store.persistStadiumRow(stadium);
        return stadium;
    }

    public synchronized Stand addStand(String stadiumId, String name) {
        Stadium stadium = stadiums.get(stadiumId);
        if (stadium == null) throw new IllegalArgumentException("Stadium not found.");
        Stand stand = new Stand(UUID.randomUUID().toString(), name);
        stadium.addStand(stand);
        store.persistStand(stadium.getId(), stand, stadium.getStands().size() - 1);
        return stand;
    }

    public synchronized Match createMatch(String home, String away,
                                          LocalDateTime kickoff, String stadiumId) {
        Stadium stadium = stadiums.get(stadiumId);
        if (stadium == null) throw new IllegalArgumentException("Stadium not found.");
        Match match = new Match(UUID.randomUUID().toString(), home, away, kickoff, stadium);
        match.putOnSale();
        matches.put(match.getId(), match);
        store.persistMatch(match);
        return match;
    }

    public List<Stadium> getStadiums() { return List.copyOf(stadiums.values()); }
    public List<Match> getMatches() { return List.copyOf(matches.values()); }
    public int stadiumCount() { return stadiums.size(); }
    public int matchCount() { return matches.size(); }
}
