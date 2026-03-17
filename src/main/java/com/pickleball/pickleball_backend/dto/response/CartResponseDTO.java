package com.pickleball.pickleball_backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDTO(
        List<CartItemDTO> items,
        BigDecimal total
) {}