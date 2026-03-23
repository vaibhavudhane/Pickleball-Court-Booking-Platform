package com.pickleball.pickleball_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingDTO(
        Long id,
        String courtName,
        String venueName,
        Long venueId,
        LocalDate bookingDate,
        String startTime,
        String endTime,
        BigDecimal amountPaid,
        String status,
        LocalDateTime bookedAt
) {}