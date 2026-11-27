package com.smartstadium.api;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import com.smartstadium.service.AccountService;
import com.smartstadium.service.CurrentAccountResolver;
import com.smartstadium.service.DemoDataService;
import com.smartstadium.service.AdminCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final CurrentAccountResolver resolver;
    private final AdminCatalogService catalog;
    private final AccountService accounts;
    private final DemoDataService demoData;

    public AdminController(CurrentAccountResolver resolver, AdminCatalogService catalog,
                           AccountService accounts, DemoDataService demoData) {
        this.resolver = resolver;
        this.catalog = catalog;
        this.accounts = accounts;
        this.demoData = demoData;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestHeader("Authorization") String auth) {
        requireAdmin(auth);
        return Map.of(
                "registeredAccounts", accounts.count(),
                "stadiums", catalog.stadiumCount(),
                "matches", catalog.matchCount(),
                "demoAvailableSeats", demoData.getStadium().getAvailableSeatCount()
        );
    }

    @GetMapping("/stadiums")
    public Object stadiums(@RequestHeader("Authorization") String auth) {
        requireAdmin(auth);
        return catalog.getStadiums();
    }

    @PostMapping("/stadiums")
    @ResponseStatus(HttpStatus.CREATED)
    public Stadium createStadium(@RequestHeader("Authorization") String auth,
                                 @Valid @RequestBody CreateStadiumRequest request) {
        requireAdmin(auth);
        return catalog.createStadium(request.name(), request.city());
    }

    @PostMapping("/stadiums/{stadiumId}/stands")
    @ResponseStatus(HttpStatus.CREATED)
    public Stand addStand(@RequestHeader("Authorization") String auth,
                          @PathVariable String stadiumId,
                          @Valid @RequestBody CreateStandRequest request) {
        requireAdmin(auth);
        return catalog.addStand(stadiumId, request.name());
    }

    @GetMapping("/matches")
    public Object matches(@RequestHeader("Authorization") String auth) {
        requireAdmin(auth);
        return catalog.getMatches();
    }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public Match createMatch(@RequestHeader("Authorization") String auth,
                             @Valid @RequestBody CreateMatchRequest request) {
        requireAdmin(auth);
        return catalog.createMatch(request.homeTeam(), request.awayTeam(),
                LocalDateTime.parse(request.kickoff()), request.stadiumId());
    }

    private void requireAdmin(String auth) {
        try {
            resolver.requireAdmin(auth);
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        }
    }

    public record CreateStadiumRequest(@NotBlank @Size(max = 100) String name,
                                       @NotBlank @Size(max = 100) String city) {}
    public record CreateStandRequest(@NotBlank @Size(max = 80) String name) {}
    public record CreateMatchRequest(@NotBlank String homeTeam, @NotBlank String awayTeam,
                                     @NotBlank String kickoff, @NotBlank String stadiumId) {}
}
