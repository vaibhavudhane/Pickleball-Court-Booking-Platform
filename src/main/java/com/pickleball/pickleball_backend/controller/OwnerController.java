package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.request.CreateVenueRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.VenueDetailDTO;
import com.pickleball.pickleball_backend.entity.Venue;
import com.pickleball.pickleball_backend.entity.VenuePhoto;
import com.pickleball.pickleball_backend.repository.VenuePhotoRepository;
import com.pickleball.pickleball_backend.repository.VenueRepository;
import com.pickleball.pickleball_backend.service.VenueService;
import com.pickleball.pickleball_backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@Tag(name = "Owner — Venue Management", description = "Court Owner endpoints — requires OWNER role")
@SecurityRequirement(name = "Bearer Authentication")
public class OwnerController {

    private final VenueService venueService;
    private final SecurityUtils securityUtils;
    private final VenuePhotoRepository venuePhotoRepository;
    private final VenueRepository venueRepository;

    @Operation(
            summary = "Get my venues",
            description = "Returns all venues owned by the logged-in Court Owner."
    )
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/venues")
    public ResponseEntity<List<VenueDetailDTO>> getMyVenues() {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(venueService.getMyVenues(ownerId));
    }

    @Operation(
            summary = "Create a new venue",
            description = "Create a venue with courts, pricing and operating hours. " +
                    "Courts are auto-created based on numCourts."
    )
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/venues")
    public ResponseEntity<VenueDetailDTO> createVenue(
            @Valid @RequestBody CreateVenueRequest request) {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(venueService.createVenue(ownerId, request));
    }

    @Operation(
            summary = "Edit venue details",
            description = "Update any venue detail. Only the owner of the venue can edit it."
    )
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/venues/{id}")
    public ResponseEntity<VenueDetailDTO> updateVenue(
            @PathVariable Long id,
            @Valid @RequestBody CreateVenueRequest request) {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(venueService.updateVenue(ownerId, id, request));
    }

    @Operation(
            summary = "View bookings at my venue",
            description = "Returns all bookings at a venue filtered by date. " +
                    "Only accessible by the venue owner."
    )
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/venues/{venueId}/bookings")
    public ResponseEntity<List<BookingDTO>> getVenueBookings(
            @PathVariable Long venueId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                venueService.getVenueBookings(ownerId, venueId, date));
    }

    @Operation(
            summary = "Upload venue photos",
            description = "Upload up to 5 photos for a venue. " +
                    "Supported formats: JPG, PNG."
    )
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/venues/{venueId}/photos")
    public ResponseEntity<Void> uploadPhotos(
            @PathVariable Long venueId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        if (files.size() > 5) {
            throw new RuntimeException("Maximum 5 photos allowed");
        }

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found"));

        String uploadDir = "uploads/venues/" + venueId + "/";
        Files.createDirectories(Paths.get(uploadDir));

        for (MultipartFile file : files) {
            String filename = UUID.randomUUID() + "_" +
                    file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.write(filePath, file.getBytes());

            VenuePhoto photo = VenuePhoto.builder()
                    .venue(venue)
                    .photoUrl("/uploads/venues/" + venueId + "/" + filename)
                    .displayOrder(0)
                    .build();
            venuePhotoRepository.save(photo);
        }

        return ResponseEntity.ok().build();
    }
}