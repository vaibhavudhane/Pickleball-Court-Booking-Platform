package com.pickleball.pickleball_backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface VenuePhotoService {
    void uploadPhotos(Long venueId, List<MultipartFile> files) throws IOException;
    void deletePhoto(Long venueId, Long photoId, Long ownerId);
}