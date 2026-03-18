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

    private static final Logger log = LoggerFactory.getLogger(AvailabilityServiceImpl.class);

    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;

    @Override
    public AvailabilityResponseDTO getAvailability(Long venueId, LocalDate date) {
        // venueId and date are not PII — safe to log
        log.debug("Fetching availability — venueId: {}, date: {}", venueId, date);

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> {
                    log.warn("Availability request failed — venueId not found: {}", venueId);
                    return new RuntimeException("Venue not found: " + venueId);
                });

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY);
        BigDecimal pricePerSlot = isWeekend
                ? venue.getWeekendRate()
                : venue.getWeekdayRate();

        log.debug("Pricing applied — venueId: {}, isWeekend: {}, price: {}",
                venueId, isWeekend, pricePerSlot);

        List<LocalTime[]> timeSlots = generateHourlySlots(
                venue.getOpeningTime(), venue.getClosingTime());

        log.debug("Generated {} time slots for venueId: {}", timeSlots.size(), venueId);

        List<Booking> existingBookings = bookingRepository
                .findByVenueIdAndBookingDateAndStatus(
                        venueId, date, BookingStatus.CONFIRMED);

        log.debug("Found {} confirmed bookings — venueId: {}, date: {}",
                existingBookings.size(), venueId, date);

        List<CourtAvailabilityDTO> courtDTOs = new ArrayList<>();

        for (Court court : venue.getCourts()) {
            List<SlotDTO> slotDTOs = new ArrayList<>();

            for (LocalTime[] slot : timeSlots) {
                LocalTime slotStart = slot[0];
                LocalTime slotEnd = slot[1];
                SlotStatus status = determineSlotStatus(
                        court, slotStart, existingBookings, date);
                slotDTOs.add(new SlotDTO(
                        slotStart.toString(), slotEnd.toString(),
                        status, pricePerSlot));
            }

            courtDTOs.add(new CourtAvailabilityDTO(
                    court.getId(), court.getCourtName(), slotDTOs));
        }

        log.info("Availability grid built — venueId: {}, date: {}, courts: {}, slotsEach: {}",
                venueId, date, courtDTOs.size(), timeSlots.size());

        return new AvailabilityResponseDTO(venueId, venue.getName(), date, courtDTOs);
    }

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
            Court court, LocalTime slotStart,
            List<Booking> existingBookings, LocalDate date) {

        if (date.isEqual(LocalDate.now())
                && slotStart.isBefore(LocalTime.now())) {
            return SlotStatus.UNAVAILABLE;
        }

        boolean isBooked = existingBookings.stream().anyMatch(booking ->
                booking.getCourt().getId().equals(court.getId()) &&
                        booking.getStartTime().equals(slotStart)
        );

        return isBooked ? SlotStatus.BOOKED : SlotStatus.AVAILABLE;
    }
}