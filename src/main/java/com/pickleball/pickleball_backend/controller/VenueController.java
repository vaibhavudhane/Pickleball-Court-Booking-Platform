package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.response.VenueCardDTO;
import com.pickleball.pickleball_backend.dto.response.VenueDetailDTO;
import com.pickleball.pickleball_backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    // Marketplace listing — with optional date/time filter
    @GetMapping
    public ResponseEntity<List<VenueCardDTO>> getAllVenues(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime startTime) {
        return ResponseEntity.ok(venueService.getAllVenues(date, startTime));
    }

    // Venue detail page
    @GetMapping("/{id}")
    public ResponseEntity<VenueDetailDTO> getVenueDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(venueService.getVenueDetail(id));
    }
}