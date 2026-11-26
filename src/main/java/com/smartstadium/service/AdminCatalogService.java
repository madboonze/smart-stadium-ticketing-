package com.smartstadium.service;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminCatalogService {
    private final Map<String, Stadium> stadiums = new ConcurrentHashMap<>();
    private final Map<String, Match> matches = new ConcurrentHashMap<>();

    public AdminCatalogService(DemoDataService demoData) {
        Stadium demo = demoData.getStadium();
        stadiums.put(demo.getId(), demo);
        matches.put(demoData.getMatch().getId(), demoData.getMatch());
    }

    public synchronized Stadium createStadium(String name, String city) {
        Stadium stadium = new Stadium(UUID.randomUUID().toString(), name, city);
        stadiums.put(stadium.getId(), stadium);
        return stadium;
    }

    public synchronized Stand addStand(String stadiumId, String name) {
        Stadium stadium = stadiums.get(stadiumId);
        if (stadium == null) throw new IllegalArgumentException("Stadium not found.");
        Stand stand = new Stand(UUID.randomUUID().toString(), name);
        stadium.addStand(stand);
        return stand;
    }

    public synchronized Match createMatch(String home, String away,
                                          LocalDateTime kickoff, String stadiumId) {
        Stadium stadium = stadiums.get(stadiumId);
        if (stadium == null) throw new IllegalArgumentException("Stadium not found.");
        Match match = new Match(UUID.randomUUID().toString(), home, away, kickoff, stadium);
        match.putOnSale();
        matches.put(match.getId(), match);
        return match;
    }

    public List<Stadium> getStadiums() { return List.copyOf(stadiums.values()); }
    public List<Match> getMatches() { return List.copyOf(matches.values()); }
    public int stadiumCount() { return stadiums.size(); }
    public int matchCount() { return matches.size(); }
}
