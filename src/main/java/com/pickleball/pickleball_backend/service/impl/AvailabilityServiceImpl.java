package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.response.*;
import com.pickleball.pickleball_backend.entity.*;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import com.pickleball.pickleball_backend.enums.SlotStatus;
import com.pickleball.pickleball_backend.repository.*;
import com.pickleball.pickleball_backend.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Logger log =
            LoggerFactory.getLogger(AvailabilityServiceImpl.class);

    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;

    @Override
    public AvailabilityResponseDTO getAvailability(Long venueId, LocalDate date) {
        log.debug("Fetching availability — venueId: {}, date: {}", venueId, date);

        if (date.isBefore(LocalDate.now())) {
            log.warn("Availability request rejected — past date: {}", date);
            throw new IllegalArgumentException(
                    "Date must not be in the past — received: " + date);
        }

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> {
                    log.warn("Availability request failed — venueId not found: {}", venueId);
                    return new RuntimeException("Venue not found: " + venueId);
                });

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY);
        BigDecimal pricePerHour = isWeekend
                ? venue.getWeekendRate()
                : venue.getWeekdayRate();

        List<Booking> existingBookings = bookingRepository
                .findByVenueIdAndBookingDateAndStatusIn(
                        venueId, date,
                        List.of(BookingStatus.CONFIRMED, BookingStatus.RESCHEDULED));

        log.debug("Found {} confirmed bookings — venueId: {}, date: {}",
                existingBookings.size(), venueId, date);

        List<CourtAvailabilityDTO> courtDTOs = new ArrayList<>();

        for (Court court : venue.getCourts()) {
            List<Booking> courtBookings = existingBookings.stream()
                    .filter(b -> b.getCourt().getId().equals(court.getId()))
                    .toList();

            List<SlotDTO> slotDTOs = generateDynamicSlots(
                    venue.getOpeningTime(),
                    venue.getClosingTime(),
                    courtBookings,
                    date,
                    pricePerHour
            );

            courtDTOs.add(new CourtAvailabilityDTO(
                    court.getId(), court.getCourtName(), slotDTOs));
        }

        log.info("Availability grid built — venueId: {}, date: {}, courts: {}",
                venueId, date, courtDTOs.size());

        return new AvailabilityResponseDTO(
                venueId, venue.getName(), date, courtDTOs);
    }

    private List<SlotDTO> generateDynamicSlots(
            LocalTime openTime,
            LocalTime closeTime,
            List<Booking> courtBookings,
            LocalDate date,
            BigDecimal pricePerHour) {

        List<SlotDTO> slots = new ArrayList<>();

        TreeSet<LocalTime> boundaries = new TreeSet<>();
        boundaries.add(openTime);
        boundaries.add(closeTime);

        for (Booking booking : courtBookings) {
            if (booking.getStartTime().isBefore(closeTime) &&
                    booking.getEndTime().isAfter(openTime)) {
                boundaries.add(booking.getStartTime());
                boundaries.add(booking.getEndTime());
            }
        }

        List<LocalTime> points = new ArrayList<>(boundaries);

        for (int i = 0; i < points.size() - 1; i++) {
            LocalTime segmentStart = points.get(i);
            LocalTime segmentEnd = points.get(i + 1);

            if (segmentStart.isBefore(openTime) || segmentEnd.isAfter(closeTime)) continue;

            boolean isBooked = isOverlappingBooking(segmentStart, segmentEnd, courtBookings);

            if (isBooked) {
                // ← FIXED: Past booked slots show as UNAVAILABLE not BOOKED
                boolean isPast = date.isEqual(LocalDate.now()) &&
                        segmentEnd.isBefore(LocalTime.now());
                SlotStatus status = isPast ? SlotStatus.UNAVAILABLE : SlotStatus.BOOKED;
                slots.add(new SlotDTO(
                        segmentStart.toString(),
                        segmentEnd.toString(),
                        status,
                        calculatePrice(pricePerHour, segmentStart, segmentEnd)));
            } else {
                slots.addAll(generateFreeSlots(segmentStart, segmentEnd, date, pricePerHour));
            }
        }

        return slots;
    }

    /**
     * Splits a free segment into 1-hour slots.
     *
     * Key fix: Instead of SKIPPING small leftover slots (< 15 min),
     * we now show them as UNAVAILABLE so every court has the same
     * time rows and the grid stays aligned.
     *
     * A slot is bookable if it is >= 30 minutes (new minimum).
     * A slot under 30 minutes shows as UNAVAILABLE (unclickable gap).
     */
    private List<SlotDTO> generateFreeSlots(
            LocalTime segmentStart,
            LocalTime segmentEnd,
            LocalDate date,
            BigDecimal pricePerHour) {

        List<SlotDTO> slots = new ArrayList<>();
        LocalTime current = segmentStart;

        while (current.isBefore(segmentEnd)) {
            LocalTime next = current.plusHours(1);

            // Clamp to segment end
            if (next.isAfter(segmentEnd)) {
                next = segmentEnd;
            }

            long minutes = Duration.between(current, next).toMinutes();

            if (minutes > 0) {
                SlotStatus status;

                if (minutes < 30) {
                    // ← FIXED: Small gap (< 30 min) — show as UNAVAILABLE
                    // so the grid row exists and courts stay aligned.
                    // Previously these were SKIPPED causing grid misalignment.
                    status = SlotStatus.UNAVAILABLE;
                } else {
                    // Normal slot — check if it's in the past for today
                    status = determineStatus(current, date);
                }

                slots.add(new SlotDTO(
                        current.toString(),
                        next.toString(),
                        status,
                        calculatePrice(pricePerHour, current, next)));
            }

            current = next;
            if (current.equals(segmentEnd)) break;
        }

        return slots;
    }

    private BigDecimal calculatePrice(
            BigDecimal pricePerHour,
            LocalTime start,
            LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        return pricePerHour
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2,
                        java.math.RoundingMode.HALF_UP);
    }

    private boolean isOverlappingBooking(
            LocalTime segmentStart,
            LocalTime segmentEnd,
            List<Booking> bookings) {
        return bookings.stream().anyMatch(b ->
                b.getStartTime().isBefore(segmentEnd) &&
                        b.getEndTime().isAfter(segmentStart));
    }

    private SlotStatus determineStatus(LocalTime slotStart, LocalDate date) {
        if (date.isEqual(LocalDate.now()) &&
                slotStart.isBefore(LocalTime.now())) {
            return SlotStatus.UNAVAILABLE;
        }
        return SlotStatus.AVAILABLE;
    }
}