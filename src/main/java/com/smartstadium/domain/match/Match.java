package com.smartstadium.domain.match;

import com.smartstadium.domain.stadium.Stadium;

import java.time.LocalDateTime;
import java.util.Objects;

public class Match {
    private final String id;
    private final String homeTeam;
    private final String awayTeam;
    private LocalDateTime kickoff;
    private final Stadium stadium;
    private MatchStatus status;

    public Match(String id, String homeTeam, String awayTeam,
                 LocalDateTime kickoff, Stadium stadium) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Match ID is required.");
        }
        if (homeTeam == null || homeTeam.isBlank()
                || awayTeam == null || awayTeam.isBlank()) {
            throw new IllegalArgumentException("Both team names are required.");
        }
        if (homeTeam.trim().equalsIgnoreCase(awayTeam.trim())) {
            throw new IllegalArgumentException("Home and away teams must be different.");
        }

        this.id = id.trim();
        this.homeTeam = homeTeam.trim();
        this.awayTeam = awayTeam.trim();
        this.kickoff = Objects.requireNonNull(kickoff, "Kickoff time is required.");
        this.stadium = Objects.requireNonNull(stadium, "Stadium is required.");
        this.status = MatchStatus.SCHEDULED;
    }

    public void putOnSale() {
        requireStatus(MatchStatus.SCHEDULED, "Only scheduled matches can be put on sale.");
        status = MatchStatus.ON_SALE;
    }

    public void markSoldOut() {
        requireStatus(MatchStatus.ON_SALE, "Only matches on sale can be marked sold out.");
        status = MatchStatus.SOLD_OUT;
    }

    public void markCompleted() {
        if (status != MatchStatus.ON_SALE && status != MatchStatus.SOLD_OUT) {
            throw new IllegalStateException("Only matches on sale or sold out can be completed.");
        }
        status = MatchStatus.COMPLETED;
    }

    public void cancel() {
        if (status == MatchStatus.COMPLETED || status == MatchStatus.CANCELLED) {
            throw new IllegalStateException("This match cannot be cancelled.");
        }
        status = MatchStatus.CANCELLED;
    }

    public void reschedule(LocalDateTime newKickoff) {
        if (status == MatchStatus.COMPLETED || status == MatchStatus.CANCELLED) {
            throw new IllegalStateException("Completed or cancelled matches cannot be rescheduled.");
        }
        this.kickoff = Objects.requireNonNull(newKickoff, "New kickoff time is required.");
    }

    private void requireStatus(MatchStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    public String getId() { return id; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public LocalDateTime getKickoff() { return kickoff; }
    public Stadium getStadium() { return stadium; }
    public MatchStatus getStatus() { return status; }
}
