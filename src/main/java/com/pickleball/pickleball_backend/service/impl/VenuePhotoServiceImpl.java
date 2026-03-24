package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.entity.Venue;
import com.pickleball.pickleball_backend.entity.VenuePhoto;
import com.pickleball.pickleball_backend.repository.VenuePhotoRepository;
import com.pickleball.pickleball_backend.repository.VenueRepository;
import com.pickleball.pickleball_backend.service.VenuePhotoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenuePhotoServiceImpl implements VenuePhotoService {

    private static final Logger log = LoggerFactory.getLogger(VenuePhotoServiceImpl.class);

    private final VenueRepository venueRepository;
    private final VenuePhotoRepository venuePhotoRepository;

    @Override
    public void uploadPhotos(Long venueId, List<MultipartFile> files) throws IOException {

        // 1. Validate venue exists
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> {
                    log.warn("Photo upload failed — venueId not found: {}", venueId);
                    return new RuntimeException("Venue not found: " + venueId);
                });

        // 2. ← Check incoming count FIRST before DB lookup
        if (files.size() > 5) {
            throw new RuntimeException(
                    "Cannot upload more than 5 photos at once");
        }

        // 2. Check existing photo count
        int existingCount = venuePhotoRepository.countByVenueId(venueId);
        int incomingCount = files.size();

        log.debug("Photo upload — venueId: {}, existing: {}, incoming: {}",
                venueId, existingCount, incomingCount);

        if (existingCount >= 5) {
            throw new RuntimeException(
                    "This venue already has 5 photos — delete some before uploading more");
        }

        if (existingCount + incomingCount > 5) {
            throw new RuntimeException(
                    "Cannot upload " + incomingCount + " photos — venue already has "
                            + existingCount + " photos. Maximum allowed is 5 total");
        }

        // 3. Validate and save each file
        String uploadDir = "uploads/venues/" + venueId + "/";
        Files.createDirectories(Paths.get(uploadDir));

        for (MultipartFile file : files) {

            // Validate extension
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();

            if (originalFilename == null ||
                    (!originalFilename.toLowerCase().endsWith(".jpg") &&
                            !originalFilename.toLowerCase().endsWith(".jpeg") &&
                            !originalFilename.toLowerCase().endsWith(".png"))) {
                throw new RuntimeException(
                        "Invalid file type: '" + originalFilename + "' — only JPG and PNG are allowed");
            }

            // Validate MIME type (catches renamed files)
            if (contentType == null ||
                    (!contentType.equals("image/jpeg") &&
                            !contentType.equals("image/png"))) {
                throw new RuntimeException(
                        "Invalid file content: '" + originalFilename + "' — file must be a real JPG or PNG image");
            }

            // Save to disk
            String filename = UUID.randomUUID() + "_" + originalFilename;
            Path filePath = Paths.get(uploadDir + filename);
            Files.write(filePath, file.getBytes());

            // Save to DB
            VenuePhoto photo = VenuePhoto.builder()
                    .venue(venue)
                    .photoUrl("/uploads/venues/" + venueId + "/" + filename)
                    .displayOrder(0)
                    .build();
            venuePhotoRepository.save(photo);

            log.debug("Photo saved — venueId: {}, filename: {}", venueId, filename);
        }

        log.info("Photos uploaded successfully — venueId: {}, count: {}", venueId, incomingCount);
    }

    @Override
    public void deletePhoto(Long venueId, Long photoId, Long ownerId) {

        // Verify venue exists and belongs to this owner
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> {
                    log.warn("Photo delete failed — venueId not found: {}", venueId);
                    return new RuntimeException("Venue not found");
                });

        if (!venue.getOwner().getId().equals(ownerId)) {
            log.warn("Photo delete rejected — not owner — venueId: {}, ownerId: {}",
                    venueId, ownerId);
            throw new RuntimeException("You do not own this venue");
        }

        // Verify photo exists and belongs to this venue
        VenuePhoto photo = venuePhotoRepository.findById(photoId)
                .orElseThrow(() -> {
                    log.warn("Photo delete failed — photoId not found: {}", photoId);
                    return new RuntimeException("Photo not found");
                });

        if (!photo.getVenue().getId().equals(venueId)) {
            log.warn("Photo delete rejected — photo does not belong to venue");
            throw new RuntimeException("Photo does not belong to this venue");
        }

        // Delete physical file from disk
        try {
            String filePath = photo.getPhotoUrl()
                    .replaceFirst("^/", ""); // remove leading slash
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            java.nio.file.Files.deleteIfExists(path);
            log.debug("Photo file deleted — path: {}", filePath);
        } catch (Exception e) {
            log.warn("Could not delete photo file — continuing with DB delete: {}", e.getMessage());
        }

        // Delete from DB
        venuePhotoRepository.delete(photo);
        log.info("Photo deleted — photoId: {}, venueId: {}", photoId, venueId);
    }

}