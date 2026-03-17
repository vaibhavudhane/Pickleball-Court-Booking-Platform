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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final CartItemRepository cartRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CheckoutResponseDTO checkout(Long userId) {
        List<CartItem> cartItems = cartRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
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
                conflicts.add(
                        item.getCourt().getCourtName() +
                                " on " + item.getBookingDate() +
                                " at " + item.getStartTime()
                );
            }
        }

        if (!conflicts.isEmpty()) {
            throw new CheckoutConflictException(
                    "These slots are no longer available: " +
                            String.join(", ", conflicts)
            );
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
        }

        cartRepository.deleteByUserId(userId);

        return new CheckoutResponseDTO(
                "Booking confirmed! " + cartItems.size() + " slot(s) booked.",
                cartItems.size()
        );
    }

    @Override
    public List<BookingDTO> getMyBookings(Long userId) {
        List<Booking> bookings = bookingRepository
                .findByUserIdOrderByBookedAtDesc(userId);
        return bookings.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public BookingDTO reschedule(Long userId, Long bookingId,
                                 RescheduleRequest request) {

        // Step 1: Find the booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found"));

        // Step 2: Verify this booking belongs to the logged-in user
        if (!booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedException(
                    "You cannot reschedule someone else's booking");
        }

        // Step 3: 12-hour rule
        // Cannot reschedule if booking starts within 12 hours from now
        LocalDateTime originalStart = booking.getBookingDate()
                .atTime(booking.getStartTime());
        LocalDateTime twelveHoursFromNow = LocalDateTime.now().plusHours(12);

        if (twelveHoursFromNow.isAfter(originalStart)) {
            throw new RescheduleNotAllowedException(
                    "Cannot reschedule. Booking starts in less than 12 hours.");
        }

        // Step 4: Check if new slot is available
        boolean newSlotTaken = bookingRepository
                .existsByCourtIdAndBookingDateAndStartTimeAndStatus(
                        booking.getCourt().getId(),
                        request.newDate(),
                        request.newStartTime(),
                        BookingStatus.CONFIRMED
                );

        if (newSlotTaken) {
            throw new SlotAlreadyBookedException(
                    "The new slot is already booked. Please choose another time.");
        }

        // Step 5: Atomic swap
        // Update the booking record — old slot freed, new slot occupied
        booking.setBookingDate(request.newDate());
        booking.setStartTime(request.newStartTime());
        booking.setEndTime(request.newStartTime().plusHours(1));
        booking.setStatus(BookingStatus.RESCHEDULED);

        Booking updated = bookingRepository.save(booking);
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