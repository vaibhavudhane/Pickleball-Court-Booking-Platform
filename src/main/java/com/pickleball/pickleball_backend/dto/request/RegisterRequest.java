package com.pickleball.pickleball_backend.dto.request;

import com.pickleball.pickleball_backend.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record RegisterRequest(

        @Schema(example = "Vaibhav Udhane")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        @Pattern(
                regexp = "^[a-zA-Z\\s]+$",
                message = "Name can only contain letters and spaces — no numbers or special characters"
        )
        String name,

        @Schema(example = "vaibhav@test.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format — example: user@email.com")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Schema(example = "Vaibhav@123", description = "Min 8 chars — must include uppercase, lowercase, number and special character (@$!%*?&)")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character (@$!%*?&)"
        )
        String password,

        @Schema(example = "OWNER", description = "Use OWNER or 1 for court owner, BOOKER or 0 for player")
        @NotNull(message = "Role is required — enter OWNER or 1 for owner, BOOKER or 0 for booker")
        Role role

) {}