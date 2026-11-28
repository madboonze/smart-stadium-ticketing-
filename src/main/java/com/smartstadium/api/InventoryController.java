package com.smartstadium.api;

import com.smartstadium.api.dto.InventoryDtos.SeatView;
import com.smartstadium.api.dto.InventoryDtos.SectionView;
import com.smartstadium.api.dto.InventoryDtos.StadiumInventoryView;
import com.smartstadium.api.dto.InventoryDtos.StadiumSummaryView;
import com.smartstadium.service.SeatInventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Public read access to the seat inventory. These are the endpoints the 3D stadium
 * calls as the customer moves from the aerial view into a single section:
 *
 * <ul>
 *   <li>{@code GET /api/stadiums}: the stadiums that can be browsed</li>
 *   <li>{@code GET /api/stadiums/{stadiumId}/inventory}: stands and sections with counts.
 *       Add {@code ?includeSeats=true} to get every row and seat in one response.</li>
 *   <li>{@code GET /api/stadiums/{stadiumId}/stands/{standId}/sections/{sectionId}}:
 *       one section with all of its rows and seats, for the zoomed in seat map</li>
 *   <li>{@code GET /api/stadiums/{stadiumId}/seats/{seatId}}: a single seat</li>
 * </ul>
 *
 * <p>No seat state is changed here. Holding and booking belong to the booking engine,
 * which resolves seats through the same service this controller reads from.</p>
 */
@RestController
@RequestMapping("/api/stadiums")
public class InventoryController {

    private final SeatInventoryService inventory;

    public InventoryController(SeatInventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public List<StadiumSummaryView> stadiums() {
        return inventory.getStadiums().stream().map(StadiumSummaryView::of).toList();
    }

    @GetMapping("/{stadiumId}/inventory")
    public StadiumInventoryView inventory(
            @PathVariable("stadiumId") String stadiumId,
            @RequestParam(name = "includeSeats", defaultValue = "false") boolean includeSeats) {
        return inventory.findStadium(stadiumId)
                .map(stadium -> StadiumInventoryView.of(stadium, includeSeats))
                .orElseThrow(() -> notFound("Stadium not found."));
    }

    @GetMapping("/{stadiumId}/stands/{standId}/sections/{sectionId}")
    public SectionView section(@PathVariable("stadiumId") String stadiumId,
                               @PathVariable("standId") String standId,
                               @PathVariable("sectionId") String sectionId) {
        return inventory.findSection(stadiumId, standId, sectionId)
                .map(section -> SectionView.of(section, true))
                .orElseThrow(() -> notFound("Section not found."));
    }

    @GetMapping("/{stadiumId}/seats/{seatId}")
    public SeatView seat(@PathVariable("stadiumId") String stadiumId,
                         @PathVariable("seatId") String seatId) {
        return inventory.findSeat(stadiumId, seatId)
                .map(SeatView::of)
                .orElseThrow(() -> notFound("Seat not found."));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
