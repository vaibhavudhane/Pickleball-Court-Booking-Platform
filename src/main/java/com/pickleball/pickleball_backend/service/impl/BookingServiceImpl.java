package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.request.RescheduleRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.CheckoutResponseDTO;
import com.pickleball.pickleball_backend.entity.Booking;
import com.pickleball.pickleball_backend.entity.CartItem;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import com.pickleball.pickleball_backend.exception.CheckoutConflictException;
import com.pickleball.pickleball_backend.exception.RescheduleNotAllowedException;
import com.pickleball.pickleball_backend.exception.ResourceNotFoundException;
import com.pickleball.pickleball_backend.exception.SlotAlreadyBookedException;
import com.pickleball.pickleball_backend.exception.UnauthorizedException;
import com.pickleball.pickleball_backend.repository.*;
import com.pickleball.pickleball_backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final CartItemRepository cartRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CheckoutResponseDTO checkout(Long userId) {
        // userId is an internal system ID — safe to log
        log.info("Checkout initiated — userId: {}", userId);

        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        log.debug("Cart contains {} items — userId: {}", cartItems.size(), userId);

        if (cartItems.isEmpty()) {
            log.warn("Checkout failed — empty cart — userId: {}", userId);
            throw new RuntimeException("Your cart is empty");
        }

        List<String> conflicts = new ArrayList<>();

        for (CartItem item : cartItems) {
            boolean alreadyBooked = bookingRepository
                    .existsByCourtIdAndBookingDateAndStartTimeAndStatus(
                            item.getCourt().getId(),
                            item.getBookingDate(),
                            item.getStartTime(),
                            BookingStatus.CONFIRMED
                    );
            if (alreadyBooked) {
                // courtId, date, time are not PII — safe to log
                String conflict = item.getCourt().getCourtName() +
                        " on " + item.getBookingDate() +
                        " at " + item.getStartTime();
                log.warn("Slot conflict detected — userId: {}, slot: {}", userId, conflict);
                conflicts.add(conflict);
            }
        }

        if (!conflicts.isEmpty()) {
            log.warn("Checkout rejected — {} conflict(s) — userId: {}",
                    conflicts.size(), userId);
            throw new CheckoutConflictException(
                    "These slots are no longer available: " +
                            String.join(", ", conflicts));
        }

        for (CartItem item : cartItems) {
            Booking booking = Booking.builder()
                    .user(userRepository.getReferenceById(userId))
                    .court(item.getCourt())
                    .venue(item.getVenue())
                    .bookingDate(item.getBookingDate())
                    .startTime(item.getStartTime())
                    .endTime(item.getEndTime())
                    .amountPaid(item.getPrice())
                    .status(BookingStatus.CONFIRMED)
                    .build();
            bookingRepository.save(booking);
            log.debug("Booking saved — courtId: {}, date: {}, time: {}",
                    item.getCourt().getId(), item.getBookingDate(), item.getStartTime());
        }

        cartRepository.deleteByUserId(userId);

        log.info("Checkout successful — userId: {}, slotsBooked: {}",
                userId, cartItems.size());

        return new CheckoutResponseDTO(
                "Booking confirmed! " + cartItems.size() + " slot(s) booked.",
                cartItems.size()
        );
    }

    @Override
    public List<BookingDTO> getMyBookings(Long userId) {
        log.debug("Fetching booking history — userId: {}", userId);
        List<Booking> bookings = bookingRepository
                .findByUserIdOrderByBookedAtDesc(userId);
        log.debug("Found {} bookings — userId: {}", bookings.size(), userId);
        return bookings.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public BookingDTO reschedule(Long userId, Long bookingId,
                                 RescheduleRequest request) {
        // userId, bookingId, date and time are not PII — safe to log
        log.info("Reschedule attempt — userId: {}, bookingId: {}, newDate: {}, newTime: {}",
                userId, bookingId, request.newDate(), request.newStartTime());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Reschedule failed — bookingId not found: {}", bookingId);
                    return new ResourceNotFoundException("Booking not found");
                });

        if (!booking.getUser().getId().equals(userId)) {
            log.warn("Reschedule rejected — unauthorized — userId: {}, bookingId: {}",
                    userId, bookingId);
            throw new UnauthorizedException(
                    "You cannot reschedule someone else's booking");
        }

        LocalDateTime originalStart = booking.getBookingDate()
                .atTime(booking.getStartTime());
        LocalDateTime twelveHoursFromNow = LocalDateTime.now().plusHours(12);

        if (twelveHoursFromNow.isAfter(originalStart)) {
            log.warn("Reschedule rejected — 12-hour rule — userId: {}, bookingId: {}",
                    userId, bookingId);
            throw new RescheduleNotAllowedException(
                    "Cannot reschedule. Booking starts in less than 12 hours.");
        }

        boolean newSlotTaken = bookingRepository
                .existsByCourtIdAndBookingDateAndStartTimeAndStatus(
                        booking.getCourt().getId(),
                        request.newDate(),
                        request.newStartTime(),
                        BookingStatus.CONFIRMED
                );

        if (newSlotTaken) {
            log.warn("Reschedule rejected — new slot taken — courtId: {}, date: {}, time: {}",
                    booking.getCourt().getId(), request.newDate(), request.newStartTime());
            throw new SlotAlreadyBookedException(
                    "The new slot is already booked. Please choose another time.");
        }

        booking.setBookingDate(request.newDate());
        booking.setStartTime(request.newStartTime());
        booking.setEndTime(request.newStartTime().plusHours(1));
        booking.setStatus(BookingStatus.RESCHEDULED);

        Booking updated = bookingRepository.save(booking);

        log.info("Booking rescheduled — userId: {}, bookingId: {}, newDate: {}, newTime: {}",
                userId, bookingId, request.newDate(), request.newStartTime());

        return toDTO(updated);
    }

    private BookingDTO toDTO(Booking b) {
        return new BookingDTO(
                b.getId(),
                b.getCourt().getCourtName(),
                b.getVenue().getName(),
                b.getVenue().getId(),
                b.getBookingDate(),
                b.getStartTime().toString(),
                b.getEndTime().toString(),
                b.getAmountPaid(),
                b.getStatus().name(),
                b.getBookedAt()
        );
    }
}