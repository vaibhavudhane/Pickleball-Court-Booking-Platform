package com.pickleball.pickleball_backend.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

public record CartItemRequest(
        Long courtId,
        Long venueId,
        LocalDate date,
        LocalTime startTime
) {}