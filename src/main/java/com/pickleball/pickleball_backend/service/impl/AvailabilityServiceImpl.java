package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.response.*;
import com.pickleball.pickleball_backend.entity.*;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import com.pickleball.pickleball_backend.enums.SlotStatus;
import com.pickleball.pickleball_backend.repository.*;
import com.pickleball.pickleball_backend.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;

    @Override
    public AvailabilityResponseDTO getAvailability(Long venueId, LocalDate date) {

        // Step 1: Fetch venue — throws exception if not found
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found: " + venueId));

        // Step 2: Determine price based on day of week
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY);
        BigDecimal pricePerSlot = isWeekend
                ? venue.getWeekendRate()
                : venue.getWeekdayRate();

        // Step 3: Generate all hourly time slots
        List<LocalTime[]> timeSlots = generateHourlySlots(
                venue.getOpeningTime(), venue.getClosingTime());

        // Step 4: Fetch ALL confirmed bookings for this venue+date
        List<Booking> existingBookings = bookingRepository
                .findByVenueIdAndBookingDateAndStatus(
                        venueId, date, BookingStatus.CONFIRMED);

        // Step 5: Build the grid
        List<CourtAvailabilityDTO> courtDTOs = new ArrayList<>();

        for (Court court : venue.getCourts()) {
            List<SlotDTO> slotDTOs = new ArrayList<>();

            for (LocalTime[] slot : timeSlots) {
                LocalTime slotStart = slot[0];
                LocalTime slotEnd   = slot[1];

                SlotStatus status = determineSlotStatus(
                        court, slotStart, existingBookings, date);

                slotDTOs.add(new SlotDTO(
                        slotStart.toString(),
                        slotEnd.toString(),
                        status,
                        pricePerSlot
                ));
            }

            courtDTOs.add(new CourtAvailabilityDTO(
                    court.getId(),
                    court.getCourtName(),
                    slotDTOs
            ));
        }

        return new AvailabilityResponseDTO(
                venueId,
                venue.getName(),
                date,
                courtDTOs
        );
    }

    // Generate slots: [06:00,07:00], [07:00,08:00], ..., [22:00,23:00]
    private List<LocalTime[]> generateHourlySlots(
            LocalTime openTime, LocalTime closeTime) {

        List<LocalTime[]> slots = new ArrayList<>();
        LocalTime current = openTime;

        while (current.isBefore(closeTime)) {
            slots.add(new LocalTime[]{ current, current.plusHours(1) });
            current = current.plusHours(1);
        }
        return slots;
    }

    private SlotStatus determineSlotStatus(
            Court court,
            LocalTime slotStart,
            List<Booking> existingBookings,
            LocalDate date) {

        // Rule 1: Past slots on TODAY are UNAVAILABLE
        if (date.isEqual(LocalDate.now())
                && slotStart.isBefore(LocalTime.now())) {
            return SlotStatus.UNAVAILABLE;
        }

        // Rule 2: Check if any confirmed booking matches this court+slot
        boolean isBooked = existingBookings.stream().anyMatch(booking ->
                booking.getCourt().getId().equals(court.getId()) &&
                        booking.getStartTime().equals(slotStart)
        );

        return isBooked ? SlotStatus.BOOKED : SlotStatus.AVAILABLE;
    }
}