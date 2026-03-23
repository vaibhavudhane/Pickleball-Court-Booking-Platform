package com.pickleball.pickleball_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "Add one or more court slots to cart in a single request")
public record AddToCartRequest(

        @Schema(description = "List of slots to add — max 10 at once")
        @NotNull(message = "Items list is required")
        @NotEmpty(message = "At least one slot must be selected")
        @Size(max = 10, message = "Cannot add more than 10 slots at once")
        @Valid
        List<CartItemRequest> items

) {}