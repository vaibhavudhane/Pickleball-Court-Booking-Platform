package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.request.AddToCartRequest;
import com.pickleball.pickleball_backend.dto.response.CartResponseDTO;
import com.pickleball.pickleball_backend.service.CartService;
import com.pickleball.pickleball_backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    // Add slots to cart
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(
            @RequestBody AddToCartRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.addToCart(userId, request);
        return ResponseEntity.ok().build();
    }

    // View current cart
    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    // Remove one item
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long itemId) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    // Clear entire cart
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        Long userId = securityUtils.getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}