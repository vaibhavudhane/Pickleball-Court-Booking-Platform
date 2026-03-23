package com.pickleball.pickleball_backend.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponseDTO(
        Long venueId,
        String venueName,
        LocalDate date,
        List<CourtAvailabilityDTO> courts
) {}
