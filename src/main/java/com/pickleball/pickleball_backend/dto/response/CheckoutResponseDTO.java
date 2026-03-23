package com.pickleball.pickleball_backend.dto.response;

public record CheckoutResponseDTO(
        String message,
        int slotsBooked
) {}