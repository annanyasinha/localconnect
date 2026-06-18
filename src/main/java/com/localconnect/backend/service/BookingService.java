package com.localconnect.backend.service;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request, String userEmail);

    List<BookingResponse> getMyBookings(String userEmail);

    List<BookingResponse> getProviderBookings(String providerEmail);
}