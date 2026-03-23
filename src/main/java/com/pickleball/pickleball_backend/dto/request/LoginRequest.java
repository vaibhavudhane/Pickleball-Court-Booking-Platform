package com.pickleball.pickleball_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LoginRequest(

        @Schema(example = "owner@test.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format — example: user@email.com")
        String email,

        @Schema(example = "Owner@123")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}