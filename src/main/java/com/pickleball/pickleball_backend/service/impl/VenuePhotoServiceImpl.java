package com.pickleball.pickleball_backend.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.pickleball.pickleball_backend.entity.Venue;
import com.pickleball.pickleball_backend.entity.VenuePhoto;
import com.pickleball.pickleball_backend.repository.VenuePhotoRepository;
import com.pickleball.pickleball_backend.repository.VenueRepository;
import com.pickleball.pickleball_backend.service.VenuePhotoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenuePhotoServiceImpl implements VenuePhotoService {

    private static final Logger log = LoggerFactory.getLogger(VenuePhotoServiceImpl.class);

    private final VenueRepository venueRepository;
    private final VenuePhotoRepository venuePhotoRepository;

    // Defaults to "none" locally — Azure sets the real value via env var
    @Value("${azure.storage.connection-string:none}")
    private String connectionString;

    @Value("${azure.storage.container-name:venue-photos}")
    private String containerName;

    private BlobContainerClient containerClient;

    @PostConstruct
    public void init() {
        if ("none".equals(connectionString)) {
            log.warn("Azure Blob Storage not configured — photo upload/delete will be unavailable. " +
                    "Set azure.storage.connection-string in application.properties to enable.");
            return;
        }
        containerClient = new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient();
        containerClient.createIfNotExists();
        log.info("Azure Blob container ready — container: {}", containerName);
    }

    @Override
    public void uploadPhotos(Long venueId, List<MultipartFile> files) throws IOException {

        if (containerClient == null) {
            throw new RuntimeException(
                    "Photo upload is not available — Azure Blob Storage is not configured.");
        }

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> {
                    log.warn("Photo upload failed — venueId not found: {}", venueId);
                    return new RuntimeException("Venue not found: " + venueId);
                });

        if (files.size() > 5) {
            throw new RuntimeException("Cannot upload more than 5 photos at once");
        }

        int existingCount = venuePhotoRepository.countByVenueId(venueId);

        if (existingCount >= 5) {
            throw new RuntimeException(
                    "This venue already has 5 photos — delete some before uploading more");
        }

        if (existingCount + files.size() > 5) {
            throw new RuntimeException(
                    "Cannot upload " + files.size() + " photos — venue already has "
                            + existingCount + " photos. Maximum allowed is 5 total");
        }

        for (MultipartFile file : files) {

            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();

            if (originalFilename == null ||
                    (!originalFilename.toLowerCase().endsWith(".jpg") &&
                            !originalFilename.toLowerCase().endsWith(".jpeg") &&
                            !originalFilename.toLowerCase().endsWith(".png"))) {
                throw new RuntimeException(
                        "Invalid file type: '" + originalFilename + "' — only JPG and PNG are allowed");
            }

            if (contentType == null ||
                    (!contentType.equals("image/jpeg") &&
                            !contentType.equals("image/png"))) {
                throw new RuntimeException(
                        "Invalid file content: '" + originalFilename + "' — file must be a real JPG or PNG image");
            }

            String blobName = "venues/" + venueId + "/" + UUID.randomUUID() + "_" + originalFilename;

            containerClient.getBlobClient(blobName)
                    .upload(file.getInputStream(), file.getSize(), true);

            String photoUrl = containerClient.getBlobClient(blobName).getBlobUrl();

            VenuePhoto photo = VenuePhoto.builder()
                    .venue(venue)
                    .photoUrl(photoUrl)
                    .displayOrder(0)
                    .build();
            venuePhotoRepository.save(photo);

            log.debug("Photo uploaded to Azure Blob — venueId: {}, blob: {}", venueId, blobName);
        }

        log.info("Photos uploaded successfully — venueId: {}, count: {}", venueId, files.size());
    }

    @Override
    public void deletePhoto(Long venueId, Long photoId, Long ownerId) {

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

        VenuePhoto photo = venuePhotoRepository.findById(photoId)
                .orElseThrow(() -> {
                    log.warn("Photo delete failed — photoId not found: {}", photoId);
                    return new RuntimeException("Photo not found");
                });

        if (!photo.getVenue().getId().equals(venueId)) {
            throw new RuntimeException("Photo does not belong to this venue");
        }

        if (containerClient != null) {
            try {
                String blobUrl = photo.getPhotoUrl();
                String blobName = blobUrl.substring(blobUrl.indexOf("venues/"));
                containerClient.getBlobClient(blobName).deleteIfExists();
                log.debug("Blob deleted — blobName: {}", blobName);
            } catch (Exception e) {
                log.warn("Could not delete blob — continuing with DB delete: {}", e.getMessage());
            }
        }

        venuePhotoRepository.delete(photo);
        log.info("Photo deleted — photoId: {}, venueId: {}", photoId, venueId);
    }
}