package com.smartstadium.api;

import com.smartstadium.api.dto.CreateMatchRequest;
import com.smartstadium.api.dto.MatchResponse;
import com.smartstadium.service.MatchManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final MatchManagementService matchManagementService;

    public MatchController(MatchManagementService matchManagementService) {
        this.matchManagementService = matchManagementService;
    }

    @GetMapping
    public List<MatchResponse> findAll() {
        return matchManagementService.findAll().stream()
                .map(MatchResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MatchResponse findById(@PathVariable String id) {
        return MatchResponse.from(matchManagementService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse create(@Valid @RequestBody CreateMatchRequest request) {
        return MatchResponse.from(matchManagementService.create(request));
    }

    @PostMapping("/{id}/on-sale")
    public MatchResponse putOnSale(@PathVariable String id) {
        return MatchResponse.from(matchManagementService.putOnSale(id));
    }

    @PostMapping("/{id}/sold-out")
    public MatchResponse markSoldOut(@PathVariable String id) {
        return MatchResponse.from(matchManagementService.markSoldOut(id));
    }

    @PostMapping("/{id}/complete")
    public MatchResponse markCompleted(@PathVariable String id) {
        return MatchResponse.from(matchManagementService.markCompleted(id));
    }

    @PostMapping("/{id}/cancel")
    public MatchResponse cancel(@PathVariable String id) {
        return MatchResponse.from(matchManagementService.cancel(id));
    }
}
