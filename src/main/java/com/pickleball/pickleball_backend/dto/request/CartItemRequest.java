package com.pickleball.pickleball_backend.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record CartItemRequest(

        @NotNull(message = "Court ID is required")
        @Positive(message = "Court ID must be a positive number")
        Long courtId,

        @NotNull(message = "Venue ID is required")
        @Positive(message = "Venue ID must be a positive number")
        Long venueId,

        @NotNull(message = "Date is required")
        @Future(message = "Booking date must be a future date — cannot book past dates")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime
) {}