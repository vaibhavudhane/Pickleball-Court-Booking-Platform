package com.pickleball.pickleball_backend.service;

import com.pickleball.pickleball_backend.dto.request.CreateVenueRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.VenueCardDTO;
import com.pickleball.pickleball_backend.dto.response.VenueDetailDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface VenueService {
    VenueDetailDTO createVenue(Long ownerId, CreateVenueRequest request);
    VenueDetailDTO updateVenue(Long ownerId, Long venueId, CreateVenueRequest request);
    VenueDetailDTO getVenueDetail(Long venueId);
    List<VenueCardDTO> getAllVenues(LocalDate date, LocalTime startTime);
    List<BookingDTO> getVenueBookings(Long ownerId, Long venueId, LocalDate date);
}