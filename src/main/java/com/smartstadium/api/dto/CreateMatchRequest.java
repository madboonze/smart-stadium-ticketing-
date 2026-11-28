package com.smartstadium.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateMatchRequest(
        @NotBlank @Size(max = 80) String homeTeam,
        @NotBlank @Size(max = 80) String awayTeam,
        @NotNull @Future LocalDateTime kickoff,
        @NotBlank String stadiumId
) {
}
