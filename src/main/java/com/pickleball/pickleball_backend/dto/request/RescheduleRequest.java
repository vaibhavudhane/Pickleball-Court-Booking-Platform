package com.pickleball.pickleball_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleRequest(

        @Schema(example = "2026-03-28", description = "New booking date in YYYY-MM-DD format — must be future date")
        @NotNull(message = "New date is required")
        @Future(message = "Reschedule date must be a future date")
        LocalDate newDate,

        @Schema(example = "10:00", description = "New start time in HH:mm format")
        @NotNull(message = "New start time is required")
        LocalTime newStartTime,

        @Schema(example = "11:30", description = "New end time in HH:mm format — minimum 1 hour after start time")
        @NotNull(message = "New end time is required")
        LocalTime newEndTime

) {}