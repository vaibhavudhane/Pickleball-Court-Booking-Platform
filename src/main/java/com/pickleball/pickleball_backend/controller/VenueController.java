package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.response.VenueCardDTO;
import com.pickleball.pickleball_backend.dto.response.VenueDetailDTO;
import com.pickleball.pickleball_backend.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
@Validated
@Tag(name = "Venues — Public", description = "Marketplace listing and venue details — no login required")
public class VenueController {

    private final VenueService venueService;

    @Operation(
            summary = "Get all venues — Marketplace",
            description = "Returns all venues. Optionally filter by date and time slot " +
                    "to show only venues with available courts."
    )
    @GetMapping
    public ResponseEntity<List<VenueCardDTO>> getAllVenues(
            @Parameter(description = "Filter by date (YYYY-MM-DD)", example = "2026-03-25")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @Parameter(description = "Filter by time slot (HH:MM)", example = "09:00",  schema = @io.swagger.v3.oas.annotations.media.Schema(
                    type = "string",
                    pattern = "^([01]\\d|2[0-3]):[0-5]\\d$"  // ← tells Swagger it's HH:mm string, not datetime
            ))
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime startTime) {
        return ResponseEntity.ok(venueService.getAllVenues(date, startTime));
    }

    @Operation(
            summary = "Get venue detail",
            description = "Returns full venue information including photos, rates, and contact details."
    )

    @GetMapping("/{id}")
    public ResponseEntity<VenueDetailDTO> getVenueDetail(
            @Parameter(description = "Venue ID", example = "1")
            @PathVariable
            @Min(value = 1, message = "Venue ID must be a positive number")
            Long id){
        return ResponseEntity.ok(venueService.getVenueDetail(id));
    }
}