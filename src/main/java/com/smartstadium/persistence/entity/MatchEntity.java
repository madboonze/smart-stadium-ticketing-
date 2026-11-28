package com.smartstadium.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Database row for a match.
 *
 * <p>The table is called {@code football_match} because {@code MATCH} is a reserved
 * keyword in SQLite. {@code kickoff} is an ISO-8601 string so the value does not depend
 * on how a driver or dialect chooses to store timestamps.</p>
 */
@Entity
@Table(name = "football_match")
public class MatchEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "stadium_id", nullable = false)
    private String stadiumId;

    @Column(name = "home_team", nullable = false)
    private String homeTeam;

    @Column(name = "away_team", nullable = false)
    private String awayTeam;

    /** ISO-8601 local date-time, for example {@code 2026-12-24T19:30}. */
    @Column(name = "kickoff", nullable = false)
    private String kickoff;

    /** {@code MatchStatus} name. */
    @Column(name = "status", nullable = false)
    private String status;

    protected MatchEntity() {
        // Required by Hibernate.
    }

    public MatchEntity(String id, String stadiumId, String homeTeam, String awayTeam,
                       String kickoff, String status) {
        this.id = id;
        this.stadiumId = stadiumId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoff = kickoff;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getStadiumId() {
        return stadiumId;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public String getKickoff() {
        return kickoff;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
