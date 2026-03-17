package com.pickleball.pickleball_backend.dto.request;

import java.util.List;

public record AddToCartRequest(
        List<CartItemRequest> items
) {}