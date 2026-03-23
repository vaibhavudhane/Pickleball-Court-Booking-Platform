package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.request.CreateVenueRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.VenueDetailDTO;
import com.pickleball.pickleball_backend.service.VenuePhotoService;
import com.pickleball.pickleball_backend.service.VenueService;
import com.pickleball.pickleball_backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@Validated
@Tag(name = "Owner — Venue Management", description = "Court Owner endpoints — requires OWNER role")
@SecurityRequirement(name = "Bearer Authentication")
public class OwnerController {

    private final VenueService venueService;
    private final SecurityUtils securityUtils;
    private final VenuePhotoService venuePhotoService;

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
            @PathVariable
            @Min(value = 1, message = "Venue ID must be a positive number")
            Long venueId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                venueService.getVenueBookings(ownerId, venueId, date));
    }



    @Operation(
            summary = "Upload venue photos",
            description = "Upload up to 5 photos for a venue. Supported formats: JPG, PNG."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = PhotoUploadRequest.class)
            )
    )
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping(value = "/venues/{venueId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhotos(
            @PathVariable Long venueId,
            @RequestPart("files") List<MultipartFile> files) throws IOException {
        venuePhotoService.uploadPhotos(venueId, files);   // ← single line call
        return ResponseEntity.ok().build();
    }



    @Operation(
            summary = "Delete a venue photo",
            description = "Delete a specific photo by ID. Only the venue owner can delete photos."
    )
    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/venues/{venueId}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable
            @Min(value = 1, message = "Venue ID must be a positive number")
            Long venueId,
            @PathVariable
            @Min(value = 1, message = "Photo ID must be a positive number")
            Long photoId) {
        Long ownerId = securityUtils.getCurrentUserId();
        venuePhotoService.deletePhoto(venueId, photoId, ownerId);
        return ResponseEntity.noContent().build();
    }

    static class PhotoUploadRequest {
        @Schema(type = "array", description = "JPG or PNG files — max 5")
        @ArraySchema(schema = @Schema(type = "string", format = "binary"))
        public List<MultipartFile> files;
    }
}