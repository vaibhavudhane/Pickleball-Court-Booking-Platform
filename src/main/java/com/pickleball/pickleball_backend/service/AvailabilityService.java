package com.pickleball.pickleball_backend.service;

import com.pickleball.pickleball_backend.dto.response.AvailabilityResponseDTO;
import java.time.LocalDate;

public interface AvailabilityService {
    AvailabilityResponseDTO getAvailability(Long venueId, LocalDate date);
}