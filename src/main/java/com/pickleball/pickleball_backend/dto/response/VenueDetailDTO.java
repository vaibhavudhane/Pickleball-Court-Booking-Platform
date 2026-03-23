package com.pickleball.pickleball_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record VenueDetailDTO(
        Long id,
        String name,
        String address,
        String description,
        Integer numCourts,
        LocalTime openingTime,
        LocalTime closingTime,
        BigDecimal weekdayRate,
        BigDecimal weekendRate,
        String contactPhone,
        String contactEmail,
        List<String> photoUrls,
        List<Long> photoIds
) {}