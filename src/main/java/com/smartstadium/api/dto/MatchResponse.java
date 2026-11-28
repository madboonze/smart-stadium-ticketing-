package com.smartstadium.api.dto;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.match.MatchStatus;

import java.time.LocalDateTime;

public record MatchResponse(
        String id,
        String homeTeam,
        String awayTeam,
        LocalDateTime kickoff,
        String stadiumId,
        String stadiumName,
        String city,
        MatchStatus status
) {
    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getKickoff(),
                match.getStadium().getId(),
                match.getStadium().getName(),
                match.getStadium().getCity(),
                match.getStatus()
        );
    }
}
