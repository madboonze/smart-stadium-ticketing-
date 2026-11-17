package com.smartstadium.api;

import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.service.DemoDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    private final DemoDataService demoDataService;

    public DemoController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping("/stadium")
    public Map<String, Object> stadium() {
        Stadium stadium = demoDataService.getStadium();

        List<Map<String, Object>> stands = stadium.getStands().stream()
                .map(stand -> Map.<String, Object>of(
                        "id", stand.getId(),
                        "name", stand.getName(),
                        "availableSeats", stand.getAvailableSeatCount(),
                        "seats", stand.getSeats().stream().map(this::seatDto).toList()
                ))
                .toList();

        return Map.of(
                "id", stadium.getId(),
                "name", stadium.getName(),
                "city", stadium.getCity(),
                "availableSeats", stadium.getAvailableSeatCount(),
                "stands", stands,
                "match", Map.of(
                        "id", demoDataService.getMatch().getId(),
                        "homeTeam", demoDataService.getMatch().getHomeTeam(),
                        "awayTeam", demoDataService.getMatch().getAwayTeam(),
                        "kickoff", demoDataService.getMatch().getKickoff().toString(),
                        "status", demoDataService.getMatch().getStatus().name()
                )
        );
    }

    private Map<String, Object> seatDto(Seat seat) {
        return Map.of(
                "id", seat.getId(),
                "label", seat.getLabel(),
                "category", seat.getCategory().name(),
                "status", seat.getStatus().name(),
                "price", seat.calculatePrice()
        );
    }
}
