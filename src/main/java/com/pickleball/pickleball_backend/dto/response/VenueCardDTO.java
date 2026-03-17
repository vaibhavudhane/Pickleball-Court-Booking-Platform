package com.pickleball.pickleball_backend.dto.response;

import java.math.BigDecimal;

public record VenueCardDTO(
        Long id,
        String name,
        String address,
        Integer numCourts,
        BigDecimal startingRate,
        String thumbnailUrl
) {}