package com.pickleball.pickleball_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AddToCartRequest(

        @NotNull(message = "Items list is required")
        @NotEmpty(message = "At least one slot must be selected")
        @Size(max = 10, message = "Cannot add more than 10 slots at once")
        @Valid
        List<CartItemRequest> items
) {}