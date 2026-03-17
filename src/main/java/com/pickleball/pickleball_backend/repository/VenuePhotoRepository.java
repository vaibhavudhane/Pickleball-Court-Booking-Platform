package com.pickleball.pickleball_backend.repository;

import com.pickleball.pickleball_backend.entity.VenuePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VenuePhotoRepository extends JpaRepository<VenuePhoto, Long> {
    List<VenuePhoto> findByVenueId(Long venueId);
}