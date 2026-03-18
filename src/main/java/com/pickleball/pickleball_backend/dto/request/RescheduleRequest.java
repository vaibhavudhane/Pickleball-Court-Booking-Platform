package com.pickleball.pickleball_backend.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleRequest(

        @NotNull(message = "New date is required")
        @Future(message = "Reschedule date must be a future date")
        LocalDate newDate,

        @NotNull(message = "New start time is required")
        LocalTime newStartTime
) {}