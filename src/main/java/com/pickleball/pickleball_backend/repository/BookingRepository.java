package com.pickleball.pickleball_backend.repository;

import com.pickleball.pickleball_backend.entity.Booking;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByVenueIdAndBookingDateAndStatus(
            Long venueId, LocalDate date, BookingStatus status);

    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    List<Booking> findByVenueIdAndBookingDateOrderByStartTime(
            Long venueId, LocalDate date);

    List<Booking> findByVenueIdAndBookingDateAndStatusIn(
            Long venueId, LocalDate date, List<BookingStatus> statuses);


    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.court.id = :courtId
        AND b.bookingDate = :date
        AND b.status IN :statuses
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        """)
    boolean existsOverlappingBooking(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<BookingStatus> statuses);
}