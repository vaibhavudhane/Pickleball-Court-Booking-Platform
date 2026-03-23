package com.pickleball.pickleball_backend.service;

import com.pickleball.pickleball_backend.dto.request.RescheduleRequest;
import com.pickleball.pickleball_backend.dto.response.BookingDTO;
import com.pickleball.pickleball_backend.dto.response.CheckoutResponseDTO;
import java.util.List;

public interface BookingService {
    CheckoutResponseDTO checkout(Long userId);
    List<BookingDTO> getMyBookings(Long userId);
    BookingDTO reschedule(Long userId, Long bookingId, RescheduleRequest request);
}