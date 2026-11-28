package com.smartstadium.api;

import com.smartstadium.api.dto.InventoryDtos.SectionView;
import com.smartstadium.domain.seat.SeatCategory;
import com.smartstadium.service.CurrentAccountResolver;
import com.smartstadium.service.SeatInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Administrator endpoints for building seat inventory.
 *
 * <p>Creating stadiums, stands and matches already lives in {@code AdminController}.
 * This controller only adds what Module 2 introduces: sections, and the rows and seats
 * generated inside them. It reuses the existing {@link CurrentAccountResolver} for the
 * admin check so there is one authorisation rule in the project, not two.</p>
 *
 * <ul>
 *   <li>{@code POST /api/admin/inventory/stadiums/{stadiumId}/stands/{standId}/sections}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final CurrentAccountResolver resolver;
    private final SeatInventoryService inventory;

    public AdminInventoryController(CurrentAccountResolver resolver, SeatInventoryService inventory) {
        this.resolver = resolver;
        this.inventory = inventory;
    }

    /**
     * Creates a section and generates all of its rows and seats.
     *
     * <p>A section of 12 rows by 24 seats produces 288 individually bookable seats,
     * labelled from the section code, for example {@code N1-A-01}.</p>
     */
    @PostMapping("/stadiums/{stadiumId}/stands/{standId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionView createSection(@RequestHeader("Authorization") String auth,
                                     @PathVariable("stadiumId") String stadiumId,
                                     @PathVariable("standId") String standId,
                                     @Valid @RequestBody CreateSectionRequest request) {
        requireAdmin(auth);
        try {
            return SectionView.of(inventory.createSection(
                    stadiumId,
                    standId,
                    request.code(),
                    request.name(),
                    request.category(),
                    request.basePrice(),
                    request.rowCount(),
                    request.seatsPerRow()), false);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private void requireAdmin(String auth) {
        try {
            resolver.requireAdmin(auth);
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        }
    }

    /**
     * @param code        short section code used in seat labels, letters and digits only
     * @param basePrice   price before the category multiplier is applied
     * @param rowCount    how many rows to generate
     * @param seatsPerRow how many seats in each row
     */
    public record CreateSectionRequest(
            @NotBlank @Size(max = 10) @Pattern(
                    regexp = "[A-Za-z0-9]+",
                    message = "Section code may contain letters and digits only.") String code,
            @NotBlank @Size(max = 80) String name,
            @NotNull SeatCategory category,
            @NotNull @Positive BigDecimal basePrice,
            @Min(1) @Max(200) int rowCount,
            @Min(1) @Max(200) int seatsPerRow) {}
}
