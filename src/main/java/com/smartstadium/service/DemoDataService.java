package com.smartstadium.service;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.seat.PremiumSeat;
import com.smartstadium.domain.seat.RegularSeat;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.VIPSeat;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.stadium.Stand;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemoDataService {
    private final Stadium stadium;
    private final Match match;

    public DemoDataService() {
        stadium = new Stadium("stadium-001", "Grand Arena", "Dubai");

        Stand north = new Stand("north", "North Stand");
        north.addSeat(new RegularSeat("N-R-01", "N-R1", new BigDecimal("1000")));
        north.addSeat(new RegularSeat("N-R-02", "N-R2", new BigDecimal("1000")));
        north.addSeat(new PremiumSeat("N-P-01", "N-P1", new BigDecimal("1000")));
        north.addSeat(new VIPSeat("N-V-01", "N-V1", new BigDecimal("1000")));

        Stand south = new Stand("south", "South Stand");
        south.addSeat(new RegularSeat("S-R-01", "S-R1", new BigDecimal("900")));
        south.addSeat(new PremiumSeat("S-P-01", "S-P1", new BigDecimal("900")));

        stadium.addStand(north);
        stadium.addStand(south);

        match = new Match(
                "match-001",
                "Dubai Falcons",
                "Abu Dhabi United",
                LocalDateTime.now().plusDays(14),
                stadium
        );
        match.putOnSale();
    }

    public Stadium getStadium() {
        return stadium;
    }

    public Match getMatch() {
        return match;
    }

    public List<Seat> getAllSeats() {
        return stadium.getStands().stream()
                .flatMap(stand -> stand.getSeats().stream())
                .toList();
    }
}
