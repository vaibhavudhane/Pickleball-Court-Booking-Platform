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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public VenueDetailDTO createVenue(Long ownerId, CreateVenueRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

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

        // Auto-create courts based on numCourts
        for (int i = 1; i <= request.numCourts(); i++) {
            Court court = Court.builder()
                    .venue(saved)
                    .courtName("Court " + i)
                    .courtNumber(i)
                    .build();
            courtRepository.save(court);
        }

        return toDetailDTO(venueRepository.findById(saved.getId()).orElseThrow());
    }

    @Override
    public VenueDetailDTO updateVenue(Long ownerId, Long venueId,
                                      CreateVenueRequest request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
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

        return toDetailDTO(venueRepository.save(venue));
    }

    @Override
    public VenueDetailDTO getVenueDetail(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        return toDetailDTO(venue);
    }

    @Override
    public List<VenueCardDTO> getAllVenues(LocalDate date, LocalTime startTime) {
        List<Venue> allVenues = venueRepository.findAll();

        if (date == null || startTime == null) {
            return allVenues.stream().map(this::toCardDTO).toList();
        }

        // Filter: only venues with at least one available court
        return allVenues.stream()
                .filter(v -> hasAvailableSlot(v, date, startTime))
                .map(this::toCardDTO)
                .toList();
    }

    @Override
    public List<BookingDTO> getVenueBookings(Long ownerId, Long venueId,
                                             LocalDate date) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        List<Booking> bookings;
        if (date != null) {
            bookings = bookingRepository
                    .findByVenueIdAndBookingDateOrderByStartTime(venueId, date);
        } else {
            bookings = bookingRepository
                    .findByVenueIdAndBookingDateOrderByStartTime(venueId,
                            LocalDate.now());
        }

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

    private boolean hasAvailableSlot(Venue v, LocalDate date,
                                     LocalTime startTime) {
        for (Court court : v.getCourts()) {
            boolean booked = bookingRepository
                    .existsByCourtIdAndBookingDateAndStartTimeAndStatus(
                            court.getId(), date, startTime, BookingStatus.CONFIRMED);
            if (!booked) return true;
        }
        return false;
    }

    private VenueCardDTO toCardDTO(Venue v) {
        String thumbnail = v.getPhotos().isEmpty() ? null
                : v.getPhotos().get(0).getPhotoUrl();
        return new VenueCardDTO(
                v.getId(), v.getName(), v.getAddress(),
                v.getNumCourts(), v.getWeekdayRate(), thumbnail
        );
    }

    private VenueDetailDTO toDetailDTO(Venue v) {
        List<String> photoUrls = v.getPhotos().stream()
                .map(VenuePhoto::getPhotoUrl)
                .toList();

        return new VenueDetailDTO(
                v.getId(), v.getName(), v.getAddress(),
                v.getDescription(), v.getNumCourts(),
                v.getOpeningTime(), v.getClosingTime(),
                v.getWeekdayRate(), v.getWeekendRate(),
                v.getContactPhone(), v.getContactEmail(),
                photoUrls
        );
    }
}