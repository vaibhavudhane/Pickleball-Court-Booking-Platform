package com.pickleball.pickleball_backend.dto.response;

public record ErrorResponse(
        String message,
        int status
) {}