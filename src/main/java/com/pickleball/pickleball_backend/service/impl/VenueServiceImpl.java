package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.request.CreateVenueRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.VenueCardDTO;
import com.pickleball.pickleball_backend.dto.response.VenueDetailDTO;
import com.pickleball.pickleball_backend.entity.*;
import com.pickleball.pickleball_backend.enums.BookingStatus;
import com.pickleball.pickleball_backend.exception.ResourceNotFoundException;
import com.pickleball.pickleball_backend.exception.UnauthorizedException;
import com.pickleball.pickleball_backend.repository.*;
import com.pickleball.pickleball_backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Transactional
public class VenueServiceImpl implements VenueService {

    private static final Logger log = LoggerFactory.getLogger(VenueServiceImpl.class);

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public VenueDetailDTO createVenue(Long ownerId, CreateVenueRequest request) {
        log.info("Create venue request — ownerId: {}", ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.warn("Venue creation failed — ownerId not found: {}", ownerId);
                    return new ResourceNotFoundException("Owner not found");
                });

        if (!request.openingTime().isBefore(request.closingTime())) {
            log.warn("Venue creation rejected — invalid hours — ownerId: {}", ownerId);
            throw new RuntimeException("Opening time must be before closing time");
        }

        if (request.closingTime().minusHours(1).isBefore(request.openingTime())) {
            throw new RuntimeException("Venue must be open for at least 1 hour");
        }

        if (request.weekendRate().compareTo(request.weekdayRate()) < 0) {
            log.warn("Venue creation rejected — invalid rates — ownerId: {}", ownerId);
            throw new RuntimeException(
                    "Weekend rate must be greater than or equal to weekday rate");
        }

        Venue venue = Venue.builder()
                .owner(owner)
                .name(request.name())
                .address(request.address())
                .description(request.description())
                .numCourts(request.numCourts())
                .openingTime(request.openingTime())
                .closingTime(request.closingTime())
                .weekdayRate(request.weekdayRate())
                .weekendRate(request.weekendRate())
                .contactPhone(request.contactPhone())
                .contactEmail(request.contactEmail())
                .build();

        Venue saved = venueRepository.save(venue);

        for (int i = 1; i <= request.numCourts(); i++) {
            Court court = Court.builder()
                    .venue(saved)
                    .courtName("Court " + i)
                    .courtNumber(i)
                    .build();
            courtRepository.save(court);
            log.debug("Court auto-created — venueId: {}, courtNumber: {}",
                    saved.getId(), i);
        }

        log.info("Venue created — venueId: {}, ownerId: {}, courts: {}",
                saved.getId(), ownerId, request.numCourts());

        return toDetailDTO(venueRepository.findById(saved.getId()).orElseThrow());
    }

    @Override
    public VenueDetailDTO updateVenue(Long ownerId, Long venueId,
                                      CreateVenueRequest request) {
        log.info("Update venue request — ownerId: {}, venueId: {}", ownerId, venueId);

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> {
                    log.warn("Venue update failed — venueId not found: {}", venueId);
                    return new ResourceNotFoundException("Venue not found");
                });

        if (!venue.getOwner().getId().equals(ownerId)) {
            log.warn("Venue update rejected — unauthorized — ownerId: {}, venueId: {}",
                    ownerId, venueId);
            throw new UnauthorizedException("You do not own this venue");
        }

        if (!request.openingTime().isBefore(request.closingTime())) {
            throw new RuntimeException("Opening time must be before closing time");
        }

        if (request.closingTime().minusHours(1).isBefore(request.openingTime())) {
            throw new RuntimeException("Venue must be open for at least 1 hour");
        }

        if (request.weekendRate().compareTo(request.weekdayRate()) < 0) {
            throw new RuntimeException(
                    "Weekend rate must be greater than or equal to weekday rate");
        }

        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setDescription(request.description());
        venue.setOpeningTime(request.openingTime());
        venue.setClosingTime(request.closingTime());
        venue.setWeekdayRate(request.weekdayRate());
        venue.setWeekendRate(request.weekendRate());
        venue.setContactPhone(request.contactPhone());
        venue.setContactEmail(request.contactEmail());

        int currentCourts = venue.getCourts().size();
        int requestedCourts = request.numCourts();

        if (requestedCourts > currentCourts) {
            for (int i = currentCourts + 1; i <= requestedCourts; i++) {
                Court court = Court.builder()
                        .venue(venue)
                        .courtName("Court " + i)
                        .courtNumber(i)
                        .build();
                courtRepository.save(court);
                log.debug("Court added during update — venueId: {}, courtNumber: {}", venueId, i);
            }
            venue.setNumCourts(requestedCourts);
        } else if (requestedCourts < currentCourts) {
            List<Court> allCourts = new ArrayList<>(venue.getCourts());
            allCourts.sort((a, b) -> b.getCourtNumber() - a.getCourtNumber());
            int toRemove = currentCourts - requestedCourts;
            for (int i = 0; i < toRemove; i++) {
                Court court = allCourts.get(i);
                venue.getCourts().remove(court);
                courtRepository.delete(court);
                log.debug("Court removed during update — venueId: {}, courtNumber: {}",
                        venueId, court.getCourtNumber());
            }
            venue.setNumCourts(requestedCourts);
            log.info("numCourts reduced — venueId: {}, from: {}, to: {}",
                    venueId, currentCourts, requestedCourts);
        }

        log.info("Venue updated — venueId: {}, ownerId: {}", venueId, ownerId);
        return toDetailDTO(venueRepository.save(venue));
    }

    @Override
    public VenueDetailDTO getVenueDetail(Long venueId) {
        log.debug("Fetching venue detail — venueId: {}", venueId);
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> {
                    log.warn("Venue not found — venueId: {}", venueId);
                    return new ResourceNotFoundException("Venue not found");
                });
        return toDetailDTO(venue);
    }

    @Override
    public List<VenueDetailDTO> getMyVenues(Long ownerId) {
        log.debug("Fetching owner venues — ownerId: {}", ownerId);
        List<Venue> venues = venueRepository.findByOwnerId(ownerId);
        log.debug("Found {} venues — ownerId: {}", venues.size(), ownerId);
        return venues.stream().map(this::toDetailDTO).toList();
    }

    @Override
    public List<VenueCardDTO> getAllVenues(LocalDate date, LocalTime startTime) {
        log.debug("Marketplace request — date: {}, startTime: {}", date, startTime);

        List<Venue> allVenues = venueRepository.findAll();
        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        if (startTime == null) {
            // ← FIXED: Remove filter — always return ALL venues
            // Just calculate availableCourts correctly for display
            List<VenueCardDTO> result = allVenues.stream()
                    .map(v -> toCardDTO(v, queryDate))
                    .toList();

            log.debug("Returning {} venues — date: {}", result.size(), queryDate);
            return result;
        }

        // Time filter — only return venues where that specific time is bookable
        List<VenueCardDTO> filtered = allVenues.stream()
                .filter(v -> hasAvailableSlot(v, queryDate, startTime))
                .map(v -> toCardDTO(v, queryDate))
                .toList();

        log.info("Marketplace filter — date: {}, time: {}, total: {}, available: {}",
                queryDate, startTime, allVenues.size(), filtered.size());
        return filtered;
    }

    @Override
    public List<BookingDTO> getVenueBookings(Long ownerId, Long venueId,
                                             LocalDate date) {
        log.debug("Venue bookings request — ownerId: {}, venueId: {}, date: {}",
                ownerId, venueId, date);

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Venue not found"));

        if (!venue.getOwner().getId().equals(ownerId)) {
            log.warn("Unauthorized venue booking access — ownerId: {}, venueId: {}",
                    ownerId, venueId);
            throw new UnauthorizedException("You do not own this venue");
        }

        List<Booking> bookings;
        if (date != null) {
            bookings = bookingRepository
                    .findByVenueIdAndBookingDateOrderByStartTime(venueId, date);
        } else {
            bookings = bookingRepository
                    .findByVenueIdAndBookingDateOrderByStartTime(
                            venueId, LocalDate.now());
        }

        log.debug("Found {} bookings — venueId: {}, date: {}",
                bookings.size(), venueId, date);

        return bookings.stream().map(b -> new BookingDTO(
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
        )).toList();
    }

    private int countAvailableCourts(Venue venue, LocalDate date) {
        int available = 0;

        List<Booking> venueBookings = bookingRepository
                .findByVenueIdAndBookingDateAndStatusIn(
                        venue.getId(), date,
                        List.of(BookingStatus.CONFIRMED, BookingStatus.RESCHEDULED));

        for (Court court : venue.getCourts()) {

            List<Booking> courtBookings = venueBookings.stream()
                    .filter(b -> b.getCourt().getId().equals(court.getId()))
                    .toList();

            TreeSet<LocalTime> boundaries = new TreeSet<>();
            boundaries.add(venue.getOpeningTime());
            boundaries.add(venue.getClosingTime());
            for (Booking b : courtBookings) {
                if (b.getStartTime().isBefore(venue.getClosingTime()) &&
                        b.getEndTime().isAfter(venue.getOpeningTime())) {
                    boundaries.add(b.getStartTime());
                    boundaries.add(b.getEndTime());
                }
            }

            List<LocalTime> points = new ArrayList<>(boundaries);
            boolean courtHasFreeSlot = false;

            for (int i = 0; i < points.size() - 1; i++) {
                LocalTime segStart = points.get(i);
                LocalTime segEnd = points.get(i + 1);

                if (segStart.isBefore(venue.getOpeningTime()) ||
                        segEnd.isAfter(venue.getClosingTime())) continue;

                boolean isBooked = courtBookings.stream().anyMatch(b ->
                        b.getStartTime().isBefore(segEnd) &&
                                b.getEndTime().isAfter(segStart));

                if (!isBooked) {
                    boolean isPast = date.isEqual(LocalDate.now()) &&
                            segStart.isBefore(LocalTime.now());
                    if (!isPast) {
                        courtHasFreeSlot = true;
                        break;
                    }
                }
            }

            if (courtHasFreeSlot) available++;
        }

        log.debug("Available courts — venueId: {}, date: {}, available: {}",
                venue.getId(), date, available);
        return available;
    }

    private boolean hasAvailableSlot(Venue v, LocalDate date, LocalTime startTime) {
        LocalTime endTime = startTime.plusHours(1);

        List<Booking> venueBookings = bookingRepository
                .findByVenueIdAndBookingDateAndStatusIn(
                        v.getId(), date,
                        List.of(BookingStatus.CONFIRMED, BookingStatus.RESCHEDULED));

        for (Court court : v.getCourts()) {
            List<Booking> courtBookings = venueBookings.stream()
                    .filter(b -> b.getCourt().getId().equals(court.getId()))
                    .toList();

            boolean isBooked = courtBookings.stream().anyMatch(b ->
                    b.getStartTime().isBefore(endTime) &&
                            b.getEndTime().isAfter(startTime));

            if (!isBooked) return true;
        }
        return false;
    }

    private VenueCardDTO toCardDTO(Venue v, LocalDate date) {
        String thumbnail = v.getPhotos().isEmpty() ? null
                : v.getPhotos().get(0).getPhotoUrl();
        int availableCourts = countAvailableCourts(v, date);
        return new VenueCardDTO(
                v.getId(),
                v.getName(),
                v.getAddress(),
                v.getNumCourts(),
                availableCourts,
                v.getWeekdayRate(),
                thumbnail
        );
    }

    private VenueDetailDTO toDetailDTO(Venue v) {
        List<String> photoUrls = v.getPhotos().stream()
                .map(VenuePhoto::getPhotoUrl)
                .toList();
        List<Long> photoIds = v.getPhotos().stream()
                .map(VenuePhoto::getId)
                .toList();
        return new VenueDetailDTO(
                v.getId(), v.getName(), v.getAddress(),
                v.getDescription(), v.getNumCourts(),
                v.getOpeningTime(), v.getClosingTime(),
                v.getWeekdayRate(), v.getWeekendRate(),
                v.getContactPhone(), v.getContactEmail(),
                photoUrls,
                photoIds
        );
    }
}