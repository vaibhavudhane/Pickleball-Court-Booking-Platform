package com.pickleball.pickleball_backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;

public record CreateVenueRequest(

        @NotBlank(message = "Venue name is required")
        @Size(min = 3, max = 100, message = "Venue name must be between 3 and 100 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\s]+$",
                message = "Venue name can only contain letters, numbers and spaces"
        )
        String name,

        @NotBlank(message = "Address is required")
        @Size(min = 10, max = 255, message = "Address must be between 10 and 255 characters")
        String address,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Number of courts is required")
        @Min(value = 1, message = "Minimum 1 court required")
        @Max(value = 20, message = "Maximum 20 courts allowed")
        Integer numCourts,

        @NotNull(message = "Opening time is required")
        LocalTime openingTime,

        @NotNull(message = "Closing time is required")
        LocalTime closingTime,

        @NotNull(message = "Weekday rate is required")
        @DecimalMin(value = "1.0", message = "Weekday rate must be at least ₹1")
        @DecimalMax(value = "100000.0", message = "Weekday rate cannot exceed ₹1,00,000")
        BigDecimal weekdayRate,

        @NotNull(message = "Weekend rate is required")
        @DecimalMin(value = "1.0", message = "Weekend rate must be at least ₹1")
        @DecimalMax(value = "100000.0", message = "Weekend rate cannot exceed ₹1,00,000")
        BigDecimal weekendRate,

        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Contact phone must be exactly 10 digits"
        )
        String contactPhone,

        @Email(message = "Invalid contact email format")
        String contactEmail
) {}