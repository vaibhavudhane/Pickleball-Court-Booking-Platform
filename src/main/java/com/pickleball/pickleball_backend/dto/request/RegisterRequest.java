package com.pickleball.pickleball_backend.dto.request;

import com.pickleball.pickleball_backend.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        @Pattern(
                regexp = "^[a-zA-Z\\s]+$",
                message = "Name can only contain letters and spaces — no numbers or special characters"
        )
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format — example: user@email.com")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character (@$!%*?&)"
        )
        String password,

        @NotNull(message = "Role is required — must be OWNER or BOOKER")
        Role role
) {}