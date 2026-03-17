package com.pickleball.pickleball_backend.dto.response;

public record AuthResponseDTO(String token, String role, String name, Long userId) {}
