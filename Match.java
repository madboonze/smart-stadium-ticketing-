package com.smartstadium.domain.match;

import com.smartstadium.domain.stadium.Stadium;

import java.time.LocalDateTime;
import java.util.Objects;

public class Match {
    private final String id;
    private final String homeTeam;
    private final String awayTeam;
    private final LocalDateTime kickoff;
    private final Stadium stadium;
    private MatchStatus status;

    public Match(String id, String homeTeam, String awayTeam,
                 LocalDateTime kickoff, Stadium stadium) {
        if (id == null || id.isBlank() || homeTeam == null || homeTeam.isBlank()
                || awayTeam == null || awayTeam.isBlank()) {
            throw new IllegalArgumentException("Match id and team names are required.");
        }
        this.id = id.trim();
        this.homeTeam = homeTeam.trim();
        this.awayTeam = awayTeam.trim();
        this.kickoff = Objects.requireNonNull(kickoff, "Kickoff time is required");
        this.stadium = Objects.requireNonNull(stadium, "Stadium is required");
        this.status = MatchStatus.SCHEDULED;
    }

    public void putOnSale() {
        if (status != MatchStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled matches can be put on sale.");
        }
        status = MatchStatus.ON_SALE;
    }

    public void cancel() {
        if (status == MatchStatus.COMPLETED) {
            throw new IllegalStateException("A completed match cannot be cancelled.");
        }
        status = MatchStatus.CANCELLED;
    }

    public String getId() { return id; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public LocalDateTime getKickoff() { return kickoff; }
    public Stadium getStadium() { return stadium; }
    public MatchStatus getStatus() { return status; }
}
