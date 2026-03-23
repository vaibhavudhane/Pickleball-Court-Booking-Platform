package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.request.AddToCartRequest;
import com.pickleball.pickleball_backend.dto.response.CartResponseDTO;
import com.pickleball.pickleball_backend.service.CartService;
import com.pickleball.pickleball_backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
@Tag(name = "Cart", description = "Cart management — add, view, remove slots")
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Add slots to cart",
            description = "Add one or more court slots to cart. " +
                    "Validates slot availability before adding. " +
                    "Supports flexible durations — e.g. 09:00 to 10:30 (1.5 hrs). " +
                    "Minimum booking duration is 1 hour."
    )
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.addToCart(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "View cart",
            description = "Get all items in cart with individual prices and total amount."
    )
    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @Operation(
            summary = "Remove one item from cart",
            description = "Remove a specific slot from cart using cart item ID."
    )
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItem(
            @Min(value = 1, message = "Cart item ID must be a positive number")
            @PathVariable Long itemId) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Clear entire cart",
            description = "Remove all items from cart at once."
    )
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        Long userId = securityUtils.getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}