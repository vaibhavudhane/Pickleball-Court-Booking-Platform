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
import java.math.RoundingMode;
import java.time.Duration;
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
        log.info("Add to cart — userId: {}, itemCount: {}",
                userId, request.items().size());

        for (CartItemRequest item : request.items()) {

            // 1. Date range check
            if (item.date().isAfter(java.time.LocalDate.now().plusDays(90))) {
                log.warn("Cart rejected — date beyond 90 days — userId: {}", userId);
                throw new RuntimeException("Cannot book more than 90 days in advance.");
            }

            // 2. End time must be after start time
            if (!item.endTime().isAfter(item.startTime())) {
                log.warn("Cart rejected — endTime not after startTime — userId: {}", userId);
                throw new RuntimeException("End time must be after start time");
            }

            // 3. Minimum 30 minutes duration
            long durationMinutes = Duration.between(
                    item.startTime(), item.endTime()).toMinutes();
            if (durationMinutes < 30) {
                log.warn("Cart rejected — duration under 30 mins — userId: {}, duration: {}",
                        userId, durationMinutes);
                throw new RuntimeException(
                        "Minimum booking duration is 30 minutes — " +
                                "your slot is only " + durationMinutes + " minutes");
            }

            // 4. Check overlaps within the same incoming request
            boolean overlapsIncomingRequest = request.items().stream()
                    .filter(other -> other != item)
                    .anyMatch(other ->
                            other.courtId().equals(item.courtId()) &&
                                    other.date().equals(item.date()) &&
                                    other.startTime().isBefore(item.endTime()) &&
                                    other.endTime().isAfter(item.startTime()));

            if (overlapsIncomingRequest) {
                log.warn("Cart rejected — items in same request overlap — userId: {}, courtId: {}",
                        userId, item.courtId());
                throw new RuntimeException(
                        "Slot " + item.startTime() + " to " + item.endTime() +
                                " on " + item.date() + " overlaps another slot in the same request");
            }

            // 5. Fetch court and validate it belongs to the venue
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

            // 6. Fetch venue
            Venue venue = venueRepository.findById(item.venueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found"));

            // 7. Start time within operating hours
            // ← FIXED: Changed minusMinutes(60) to minusMinutes(30)
            // This allows booking start times up to 30 mins before closing
            // (minimum 30-min booking), instead of wrongly requiring 60 mins gap
            if (item.startTime().isBefore(venue.getOpeningTime()) ||
                    item.startTime().isAfter(venue.getClosingTime().minusMinutes(30))) {
                log.warn("Cart rejected — startTime outside operating hours — venueId: {}, time: {}",
                        item.venueId(), item.startTime());
                throw new RuntimeException(
                        "Start time " + item.startTime() +
                                " is outside venue operating hours (" +
                                venue.getOpeningTime() + " - " + venue.getClosingTime() + ")");
            }

            // 8. End time must not exceed venue closing time
            if (item.endTime().isAfter(venue.getClosingTime())) {
                log.warn("Cart rejected — endTime exceeds closing — venueId: {}, endTime: {}",
                        item.venueId(), item.endTime());
                throw new RuntimeException(
                        "End time " + item.endTime() +
                                " exceeds venue closing time " + venue.getClosingTime());
            }

            // 9. Check overlap against confirmed/rescheduled bookings in DB
            if (bookingRepository.existsOverlappingBooking(
                    item.courtId(), item.date(),
                    item.startTime(), item.endTime(),
                    List.of(BookingStatus.CONFIRMED, BookingStatus.RESCHEDULED))) {
                log.warn("Cart rejected — slot overlaps existing booking — courtId: {}, date: {}, time: {} to {}",
                        item.courtId(), item.date(), item.startTime(), item.endTime());
                throw new RuntimeException(
                        "Slot " + item.startTime() + " to " + item.endTime() +
                                " on " + item.date() + " overlaps an existing booking");
            }

            // 10. Check overlap against items already in this user's cart in DB
            List<CartItem> existingCartItems = cartRepository.findByUserId(userId);
            boolean overlapsCart = existingCartItems.stream()
                    .anyMatch(cartItem ->
                            cartItem.getCourt().getId().equals(item.courtId()) &&
                                    cartItem.getBookingDate().equals(item.date()) &&
                                    cartItem.getStartTime().isBefore(item.endTime()) &&
                                    cartItem.getEndTime().isAfter(item.startTime()));

            if (overlapsCart) {
                log.warn("Cart rejected — overlaps item in cart — userId: {}, courtId: {}, time: {} to {}",
                        userId, item.courtId(), item.startTime(), item.endTime());
                throw new RuntimeException(
                        "Slot " + item.startTime() + " to " + item.endTime() +
                                " on " + item.date() + " overlaps another slot already in your cart");
            }

            // 11. Exact duplicate check
            if (cartRepository.existsByUserIdAndCourtIdAndBookingDateAndStartTime(
                    userId, item.courtId(), item.date(), item.startTime())) {
                log.warn("Cart rejected — slot already in cart — userId: {}, courtId: {}",
                        userId, item.courtId());
                throw new RuntimeException("Slot already in your cart");
            }

            // 12. Calculate price — multiply first, divide last, round only at end
            DayOfWeek day = item.date().getDayOfWeek();
            boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
            BigDecimal hourlyRate = isWeekend
                    ? venue.getWeekendRate()
                    : venue.getWeekdayRate();

            BigDecimal price = hourlyRate
                    .multiply(BigDecimal.valueOf(durationMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            // 13. Save cart item
            CartItem cartItem = CartItem.builder()
                    .user(userRepository.getReferenceById(userId))
                    .court(courtRepository.getReferenceById(item.courtId()))
                    .venue(venue)
                    .bookingDate(item.date())
                    .startTime(item.startTime())
                    .endTime(item.endTime())
                    .price(price)
                    .build();

            cartRepository.save(cartItem);
            log.debug("Cart item saved — userId: {}, courtId: {}, date: {}, " +
                            "startTime: {}, endTime: {}, durationMins: {}, price: {}",
                    userId, item.courtId(), item.date(),
                    item.startTime(), item.endTime(), durationMinutes, price);
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
        List<CartItem> items = cartRepository.findByUserId(userId);
        if (items.isEmpty()) {
            log.warn("Clear cart called on empty cart — userId: {}", userId);
            throw new RuntimeException("Your cart is already empty");
        }
        cartRepository.deleteByUserId(userId);
        log.info("Cart cleared — userId: {}", userId);
    }
}