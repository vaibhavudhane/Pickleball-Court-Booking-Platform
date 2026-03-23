package com.pickleball.pickleball_backend.dto.response;

import com.pickleball.pickleball_backend.enums.SlotStatus;
import java.math.BigDecimal;

public record SlotDTO(
        String startTime,
        String endTime,
        SlotStatus status,
        BigDecimal price
) {}