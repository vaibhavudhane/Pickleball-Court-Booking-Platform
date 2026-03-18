package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.request.AddToCartRequest;
import com.pickleball.pickleball_backend.dto.request.CartItemRequest;
import com.pickleball.pickleball_backend.dto.response.CartItemDTO;
import com.pickleball.pickleball_backend.dto.response.CartResponseDTO;
import com.pickleball.pickleball_backend.entity.CartItem;
import com.pickleball.pickleball_backend.entity.Court;
import com.pickleball.pickleball_backend.entity.Venue;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import com.pickleball.pickleball_backend.repository.*;
import com.pickleball.pickleball_backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartItemRepository cartRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    @Override
    public void addToCart(Long userId, AddToCartRequest request) {
        // userId and item count are not PII — safe to log
        log.info("Add to cart — userId: {}, itemCount: {}",
                userId, request.items().size());

        for (CartItemRequest item : request.items()) {

            if (item.date().isBefore(java.time.LocalDate.now())) {
                log.warn("Cart rejected — past date — userId: {}, date: {}",
                        userId, item.date());
                throw new RuntimeException(
                        "Cannot book a slot in the past. Please select a future date.");
            }

            if (item.date().isAfter(java.time.LocalDate.now().plusDays(90))) {
                log.warn("Cart rejected — date beyond 90 days — userId: {}", userId);
                throw new RuntimeException(
                        "Cannot book more than 90 days in advance.");
            }

            if (item.startTime().getMinute() != 0
                    || item.startTime().getSecond() != 0) {
                log.warn("Cart rejected — non-hour time — userId: {}, time: {}",
                        userId, item.startTime());
                throw new RuntimeException(
                        "Booking time must be on the hour — e.g. 09:00, 10:00, not 09:30");
            }

            if (item.startTime().getHour() < 0
                    || item.startTime().getHour() > 22) {
                log.warn("Cart rejected — time out of range — userId: {}, time: {}",
                        userId, item.startTime());
                throw new RuntimeException(
                        "Invalid booking time. Time must be between 00:00 and 22:00");
            }

            if (item.courtId() <= 0) {
                throw new RuntimeException("Invalid Court ID");
            }
            if (item.venueId() <= 0) {
                throw new RuntimeException("Invalid Venue ID");
            }

            Court court = courtRepository.findById(item.courtId())
                    .orElseThrow(() -> new RuntimeException(
                            "Court not found with ID: " + item.courtId()));

            if (!court.getVenue().getId().equals(item.venueId())) {
                log.warn("Cart rejected — court/venue mismatch — courtId: {}, venueId: {}",
                        item.courtId(), item.venueId());
                throw new RuntimeException(
                        "Court " + item.courtId() +
                                " does not belong to venue " + item.venueId());
            }

            Venue venueCheck = venueRepository.findById(item.venueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found"));

            if (item.startTime().isBefore(venueCheck.getOpeningTime()) ||
                    item.startTime().isAfter(
                            venueCheck.getClosingTime().minusHours(1))) {
                log.warn("Cart rejected — outside operating hours — venueId: {}, time: {}",
                        item.venueId(), item.startTime());
                throw new RuntimeException(
                        "Slot time " + item.startTime() +
                                " is outside venue operating hours (" +
                                venueCheck.getOpeningTime() + " - " +
                                venueCheck.getClosingTime() + ")");
            }

            if (bookingRepository
                    .existsByCourtIdAndBookingDateAndStartTimeAndStatus(
                            item.courtId(), item.date(),
                            item.startTime(), BookingStatus.CONFIRMED)) {
                log.warn("Cart rejected — slot already booked — courtId: {}, date: {}, time: {}",
                        item.courtId(), item.date(), item.startTime());
                throw new RuntimeException(
                        "Slot already booked: " + item.startTime() +
                                " on " + item.date());
            }

            if (cartRepository
                    .existsByUserIdAndCourtIdAndBookingDateAndStartTime(
                            userId, item.courtId(),
                            item.date(), item.startTime())) {
                log.warn("Cart rejected — slot already in cart — userId: {}, courtId: {}",
                        userId, item.courtId());
                throw new RuntimeException("Slot already in your cart");
            }

            Venue venue = venueRepository.findById(item.venueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found"));

            DayOfWeek day = item.date().getDayOfWeek();
            boolean isWeekend = (day == DayOfWeek.SATURDAY
                    || day == DayOfWeek.SUNDAY);
            BigDecimal price = isWeekend
                    ? venue.getWeekendRate()
                    : venue.getWeekdayRate();

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
            log.debug("Cart item saved — userId: {}, courtId: {}, date: {}, time: {}",
                    userId, item.courtId(), item.date(), item.startTime());
        }

        log.info("Cart updated — userId: {}, itemsAdded: {}",
                userId, request.items().size());
    }

    @Override
    public CartResponseDTO getCart(Long userId) {
        log.debug("Fetching cart — userId: {}", userId);
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

        BigDecimal total = dtos.stream()
                .map(CartItemDTO::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Cart retrieved — userId: {}, items: {}, total: {}",
                userId, dtos.size(), total);
        return new CartResponseDTO(dtos, total);
    }

    @Override
    public void removeItem(Long userId, Long cartItemId) {
        log.info("Remove cart item — userId: {}, cartItemId: {}",
                userId, cartItemId);

        CartItem item = cartRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.warn("Cart item not found — cartItemId: {}", cartItemId);
                    return new RuntimeException("Cart item not found");
                });

        if (!item.getUser().getId().equals(userId)) {
            log.warn("Unauthorized cart removal — userId: {}, cartItemId: {}",
                    userId, cartItemId);
            throw new RuntimeException("Not your cart item");
        }

        cartRepository.delete(item);
        log.info("Cart item removed — userId: {}, cartItemId: {}",
                userId, cartItemId);
    }

    @Override
    public void clearCart(Long userId) {
        log.info("Clearing cart — userId: {}", userId);
        cartRepository.deleteByUserId(userId);
        log.info("Cart cleared — userId: {}", userId);
    }
}