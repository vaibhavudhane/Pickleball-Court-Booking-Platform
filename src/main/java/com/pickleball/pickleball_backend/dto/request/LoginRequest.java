package com.pickleball.pickleball_backend.dto.request;

import jakarta.validation.constraints.*;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format — example: user@email.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}