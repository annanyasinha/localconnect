package com.localconnect.backend.service;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ServiceListingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request, String userEmail);

    List<BookingResponse> getMyBookings(String userEmail);

    List<BookingResponse> getProviderBookings(String providerEmail);

    BookingResponse cancelBooking(Long bookingId, String userEmail);

    BookingResponse rescheduleBooking(Long bookingId, String newBookingDateStr, String userEmail);

    BookingResponse getBookingStatus(Long bookingId, String userEmail);

    List<ServiceListingResponse> recommendServices(String category, String city);
}