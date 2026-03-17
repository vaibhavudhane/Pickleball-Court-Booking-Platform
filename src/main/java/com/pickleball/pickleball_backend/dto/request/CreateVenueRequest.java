package com.pickleball.pickleball_backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;

public record CreateVenueRequest(
        @NotBlank(message = "Venue name is required")
        String name,

        @NotBlank(message = "Address is required")
        String address,

        String description,

        @Min(value = 1, message = "Minimum 1 court required")
        Integer numCourts,

        @NotNull(message = "Opening time is required")
        LocalTime openingTime,

        @NotNull(message = "Closing time is required")
        LocalTime closingTime,

        @NotNull(message = "Weekday rate is required")
        BigDecimal weekdayRate,

        @NotNull(message = "Weekend rate is required")
        BigDecimal weekendRate,

        String contactPhone,

        String contactEmail
) {}