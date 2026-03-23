package com.pickleball.pickleball_backend.dto.response;

import java.math.BigDecimal;

public record VenueCardDTO(
        Long id,
        String name,
        String address,
        Integer totalCourts,
        Integer availableCourts,    // ← how many courts have at least one free slot
        BigDecimal startingRate,
        String thumbnailUrl
) {}