package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.request.RescheduleRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.CheckoutResponseDTO;
import com.pickleball.pickleball_backend.service.BookingService;
import com.pickleball.pickleball_backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Checkout, booking history and rescheduling")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Checkout — book all cart items",
            description = "Atomically books all slots in cart. " +
                    "If any slot is taken, entire checkout is rejected. " +
                    "Handles concurrent bookings safely."
    )
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDTO> checkout() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(bookingService.checkout(userId));
    }

    @Operation(
            summary = "My Bookings — booking history",
            description = "Returns all confirmed and rescheduled bookings " +
                    "for the logged-in user, sorted newest first."
    )
    @GetMapping("/my")
    public ResponseEntity<List<BookingDTO>> myBookings() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    @Operation(
            summary = "Reschedule a booking",
            description = "Reschedule a confirmed booking to a new date and time. " +
                    "Only allowed if booking starts more than 12 hours from now. " +
                    "New slot must be available."
    )
    @PutMapping("/{bookingId}/reschedule")
    public ResponseEntity<BookingDTO> reschedule(
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                bookingService.reschedule(userId, bookingId, request));
    }
}