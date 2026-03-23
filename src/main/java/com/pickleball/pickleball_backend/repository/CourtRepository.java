package com.pickleball.pickleball_backend.repository;

import com.pickleball.pickleball_backend.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByVenueId(Long venueId);
}
