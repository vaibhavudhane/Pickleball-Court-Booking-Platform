package com.pickleball.pickleball_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleRequest(
        @NotNull(message = "New date is required")
        LocalDate newDate,

        @NotNull(message = "New start time is required")
        LocalTime newStartTime
) {}