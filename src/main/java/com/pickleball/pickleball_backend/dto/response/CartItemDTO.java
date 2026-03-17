package com.pickleball.pickleball_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CartItemDTO(
        Long cartItemId,
        String courtName,
        String venueName,
        Long venueId,
        LocalDate bookingDate,
        String startTime,
        String endTime,
        BigDecimal price
) {}
