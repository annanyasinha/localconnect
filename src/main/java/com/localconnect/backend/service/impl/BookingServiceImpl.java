package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.entity.Booking;
import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.ApprovalStatus;
import com.localconnect.backend.enums.BookingStatus;
import com.localconnect.backend.mapper.BookingMapper;
import com.localconnect.backend.repository.BookingRepository;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceListingRepository serviceListingRepository;
    private final BookingMapper bookingMapper;

    @Override
    public BookingResponse createBooking(BookingCreateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ServiceListing serviceListing = serviceListingRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (serviceListing.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("Service is not approved");
        }

        LocalDateTime bookingDate = request.getBookingDate() != null
                ? request.getBookingDate()
                : LocalDateTime.now();

        Booking booking = Booking.builder()
                .user(user)
                .serviceListing(serviceListing)
                .message(request.getMessage())
                .bookingDate(bookingDate)
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingRepository.findByUser(user)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Override
    public List<BookingResponse> getProviderBookings(String providerEmail) {
        User provider = userRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        List<ServiceListing> myServices = serviceListingRepository.findByProvider(provider);
        List<Booking> allBookings = new ArrayList<>();

        for (ServiceListing service : myServices) {
            allBookings.addAll(bookingRepository.findByServiceListing(service));
        }

        return allBookings.stream()
                .map(bookingMapper::toResponse)
                .toList();
    }
}