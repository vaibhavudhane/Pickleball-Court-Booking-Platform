package com.pickleball.pickleball_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record CartItemRequest(

        @Schema(example = "1", description = "Court ID from availability grid")
        @NotNull(message = "Court ID is required")
        @Positive(message = "Court ID must be a positive number")
        Long courtId,

        @Schema(example = "1", description = "Venue ID")
        @NotNull(message = "Venue ID is required")
        @Positive(message = "Venue ID must be a positive number")
        Long venueId,

        @Schema(example = "2026-03-25", description = "Booking date in YYYY-MM-DD format — must be future date")
        @NotNull(message = "Date is required")
        @Future(message = "Booking date must be a future date — cannot book past dates")
        LocalDate date,

        @Schema(example = "09:00", description = "Start time in HH:mm format")
        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @Schema(example = "10:30", description = "End time in HH:mm format — minimum 1 hour after start time")
        @NotNull(message = "End time is required")
        LocalTime endTime

) {}