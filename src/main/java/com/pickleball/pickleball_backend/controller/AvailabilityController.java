package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.response.AvailabilityResponseDTO;
import com.pickleball.pickleball_backend.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    // URL: GET /api/venues/1/availability?date=2024-06-15
    @GetMapping("/api/venues/{venueId}/availability")
    public ResponseEntity<AvailabilityResponseDTO> getAvailability(
            @PathVariable Long venueId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(
                availabilityService.getAvailability(venueId, date));
    }
}