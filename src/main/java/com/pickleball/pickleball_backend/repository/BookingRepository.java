package com.pickleball.pickleball_backend.repository;

import com.pickleball.pickleball_backend.entity.Booking;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Check if a specific slot is already booked
    boolean existsByCourtIdAndBookingDateAndStartTimeAndStatus(
            Long courtId, LocalDate bookingDate, LocalTime startTime, BookingStatus status);

    // Get all confirmed bookings for a venue on a date (for availability grid)
    List<Booking> findByVenueIdAndBookingDateAndStatus(
            Long venueId, LocalDate bookingDate, BookingStatus status);

    // Get all bookings for a user (My Bookings page)
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    // Get all bookings at a venue on a date (Owner dashboard)
    List<Booking> findByVenueIdAndBookingDateOrderByStartTime(
            Long venueId, LocalDate bookingDate);
}