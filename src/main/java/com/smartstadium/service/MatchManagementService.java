package com.smartstadium.service;

import com.smartstadium.api.dto.CreateMatchRequest;
import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.stadium.Stadium;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchManagementService {
    private final Map<String, Match> matches = new ConcurrentHashMap<>();
    private final DemoDataService demoDataService;
    private final AdminCatalogService adminCatalogService;

    public MatchManagementService(DemoDataService demoDataService,
                                 AdminCatalogService adminCatalogService) {
        this.demoDataService = demoDataService;
        this.adminCatalogService = adminCatalogService;
        Match sample = demoDataService.getMatch();
        matches.put(sample.getId(), sample);
    }

    public List<Match> findAll() {
        return matches.values().stream()
                .sorted(Comparator.comparing(Match::getKickoff))
                .toList();
    }

    public Match findById(String id) {
        Match match = matches.get(id);
        if (match == null) {
            throw new MatchNotFoundException(id);
        }
        return match;
    }

    public Match create(CreateMatchRequest request) {
        if (request.homeTeam().trim().equalsIgnoreCase(request.awayTeam().trim())) {
            throw new IllegalArgumentException("Home and away teams must be different.");
        }

        Stadium stadium = findStadium(request.stadiumId());
        String id = UUID.randomUUID().toString();

        Match match = new Match(
                id,
                request.homeTeam(),
                request.awayTeam(),
                request.kickoff(),
                stadium
        );

        matches.put(id, match);
        return match;
    }

    public Match putOnSale(String id) {
        Match match = findById(id);
        match.putOnSale();
        return match;
    }

    public Match markSoldOut(String id) {
        Match match = findById(id);
        match.markSoldOut();
        return match;
    }

    public Match markCompleted(String id) {
        Match match = findById(id);
        match.markCompleted();
        return match;
    }

    public Match cancel(String id) {
        Match match = findById(id);
        match.cancel();
        return match;
    }

    private Stadium findStadium(String stadiumId) {
        return adminCatalogService.getStadiums().stream()
                .filter(stadium -> stadium.getId().equals(stadiumId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown stadium ID: " + stadiumId));
    }
}
