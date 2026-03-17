package com.pickleball.pickleball_backend.dto.response;

import java.util.List;

public record CourtAvailabilityDTO(
        Long courtId,
        String courtName,
        List<SlotDTO> slots
) {}