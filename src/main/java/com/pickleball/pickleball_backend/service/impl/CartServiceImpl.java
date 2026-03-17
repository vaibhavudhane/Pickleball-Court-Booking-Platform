package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.request.AddToCartRequest;
import com.pickleball.pickleball_backend.dto.request.CartItemRequest;
import com.pickleball.pickleball_backend.dto.response.CartItemDTO;
import com.pickleball.pickleball_backend.dto.response.CartResponseDTO;
import com.pickleball.pickleball_backend.entity.CartItem;
import com.pickleball.pickleball_backend.entity.Venue;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import com.pickleball.pickleball_backend.repository.*;
import com.pickleball.pickleball_backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    @Override
    public void addToCart(Long userId, AddToCartRequest request) {
        for (CartItemRequest item : request.items()) {

            // Guard 1: Is this slot already booked by someone?
            if (bookingRepository.existsByCourtIdAndBookingDateAndStartTimeAndStatus(
                    item.courtId(), item.date(),
                    item.startTime(), BookingStatus.CONFIRMED)) {
                throw new RuntimeException(
                        "Slot already booked: " + item.startTime() + " on " + item.date());
            }

            // Guard 2: Is this slot already in THIS user's cart?
            if (cartRepository.existsByUserIdAndCourtIdAndBookingDateAndStartTime(
                    userId, item.courtId(), item.date(), item.startTime())) {
                throw new RuntimeException("Slot already in your cart");
            }

            // Calculate price based on day of week
            Venue venue = venueRepository.findById(item.venueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found"));

            DayOfWeek day = item.date().getDayOfWeek();
            boolean isWeekend = (day == DayOfWeek.SATURDAY
                    || day == DayOfWeek.SUNDAY);
            BigDecimal price = isWeekend
                    ? venue.getWeekendRate()
                    : venue.getWeekdayRate();

            // Save cart item
            CartItem cartItem = CartItem.builder()
                    .user(userRepository.getReferenceById(userId))
                    .court(courtRepository.getReferenceById(item.courtId()))
                    .venue(venue)
                    .bookingDate(item.date())
                    .startTime(item.startTime())
                    .endTime(item.startTime().plusHours(1))
                    .price(price)
                    .build();

            cartRepository.save(cartItem);
        }
    }

    @Override
    public CartResponseDTO getCart(Long userId) {
        List<CartItem> items = cartRepository.findByUserId(userId);

        List<CartItemDTO> dtos = items.stream().map(item -> new CartItemDTO(
                item.getId(),
                item.getCourt().getCourtName(),
                item.getVenue().getName(),
                item.getVenue().getId(),
                item.getBookingDate(),
                item.getStartTime().toString(),
                item.getEndTime().toString(),
                item.getPrice()
        )).toList();

        // Calculate total
        BigDecimal total = dtos.stream()
                .map(CartItemDTO::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDTO(dtos, total);
    }

    @Override
    public void removeItem(Long userId, Long cartItemId) {
        CartItem item = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Security check — make sure this item belongs to this user
        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not your cart item");
        }
        cartRepository.delete(item);
    }

    @Override
    public void clearCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }
}