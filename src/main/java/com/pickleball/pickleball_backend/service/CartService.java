package com.pickleball.pickleball_backend.service;

import com.pickleball.pickleball_backend.dto.request.AddToCartRequest;
import com.pickleball.pickleball_backend.dto.response.CartResponseDTO;

public interface CartService {
    void addToCart(Long userId, AddToCartRequest request);
    CartResponseDTO getCart(Long userId);
    void removeItem(Long userId, Long cartItemId);
    void clearCart(Long userId);
}