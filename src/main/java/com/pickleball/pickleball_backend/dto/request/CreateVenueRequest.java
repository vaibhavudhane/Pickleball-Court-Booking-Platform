package com.pickleball.pickleball_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
        import java.math.BigDecimal;
import java.time.LocalTime;

@Schema(description = "Request body to create or update a venue")
public record CreateVenueRequest(

        @Schema(example = "Smash Arena", description = "Venue name — letters, numbers and spaces only")
        @NotBlank(message = "Venue name is required")
        @Size(min = 3, max = 100, message = "Venue name must be between 3 and 100 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\s]+$",
                message = "Venue name can only contain letters, numbers and spaces"
        )
        String name,

        @Schema(example = "Baner Road, Pune, Maharashtra")
        @NotBlank(message = "Address is required")
        @Size(min = 10, max = 255, message = "Address must be between 10 and 255 characters")
        String address,

        @Schema(example = "Premium pickleball courts with parking and cafeteria")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Schema(example = "3", description = "Number of courts — courts are auto-created")
        @NotNull(message = "Number of courts is required")
        @Min(value = 1, message = "Minimum 1 court required")
        @Max(value = 20, message = "Maximum 20 courts allowed")
        Integer numCourts,

        @Schema(example = "06:00", description = "Opening time in HH:mm format")
        @NotNull(message = "Opening time is required")
        LocalTime openingTime,

        @Schema(example = "23:00", description = "Closing time in HH:mm format")
        @NotNull(message = "Closing time is required")
        LocalTime closingTime,

        @Schema(example = "500", description = "Price per hour on weekdays in ₹")
        @NotNull(message = "Weekday rate is required")
        @DecimalMin(value = "1.0", message = "Weekday rate must be at least ₹1")
        @DecimalMax(value = "100000.0", message = "Weekday rate cannot exceed ₹1,00,000")
        BigDecimal weekdayRate,

        @Schema(example = "700", description = "Price per hour on weekends in ₹")
        @NotNull(message = "Weekend rate is required")
        @DecimalMin(value = "1.0", message = "Weekend rate must be at least ₹1")
        @DecimalMax(value = "100000.0", message = "Weekend rate cannot exceed ₹1,00,000")
        BigDecimal weekendRate,

        @Schema(example = "9876543210", description = "10-digit contact phone number")
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Contact phone must be exactly 10 digits"
        )
        String contactPhone,

        @Schema(example = "smash@arena.com", description = "Contact email address")
        @Email(message = "Invalid contact email format")
        String contactEmail

) {}