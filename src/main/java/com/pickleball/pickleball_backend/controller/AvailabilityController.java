package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.response.AvailabilityResponseDTO;
import com.pickleball.pickleball_backend.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Court availability grid endpoint")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Operation(
            summary = "Get court availability grid",
            description = "Returns colour-coded slot grid for all courts at a venue on a specific date. " +
                    "Shows AVAILABLE, BOOKED, and UNAVAILABLE slots with pricing."
    )
    @GetMapping("/api/venues/{venueId}/availability")
    public ResponseEntity<AvailabilityResponseDTO> getAvailability(
            @Parameter(description = "Venue ID", example = "1")
            @PathVariable Long venueId,
            @Parameter(description = "Date in YYYY-MM-DD format", example = "2026-03-25")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(
                availabilityService.getAvailability(venueId, date));
    }
}