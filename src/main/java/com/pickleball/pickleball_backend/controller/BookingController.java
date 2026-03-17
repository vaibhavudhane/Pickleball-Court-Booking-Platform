package com.pickleball.pickleball_backend.controller;

import com.pickleball.pickleball_backend.dto.request.RescheduleRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.CheckoutResponseDTO;
import com.pickleball.pickleball_backend.service.BookingService;
import com.pickleball.pickleball_backend.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SecurityUtils securityUtils;

    // Checkout
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDTO> checkout() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(bookingService.checkout(userId));
    }

    // My Bookings
    @GetMapping("/my")
    public ResponseEntity<List<BookingDTO>> myBookings() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    // Reschedule
    @PutMapping("/{bookingId}/reschedule")
    public ResponseEntity<BookingDTO> reschedule(
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                bookingService.reschedule(userId, bookingId, request));
    }
}