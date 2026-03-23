package com.pickleball.pickleball_backend.repository;

import com.pickleball.pickleball_backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    boolean existsByUserIdAndCourtIdAndBookingDateAndStartTime(
            Long userId, Long courtId, LocalDate date, LocalTime startTime);
    void deleteByUserId(Long userId);  // Clears entire cart after checkout
}

