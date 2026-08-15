package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.entity.Booking;
import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.ApprovalStatus;
import com.localconnect.backend.enums.BookingStatus;
import com.localconnect.backend.enums.RoleName;
import com.localconnect.backend.mapper.BookingMapper;
import com.localconnect.backend.mapper.ServiceListingMapper;
import com.localconnect.backend.repository.BookingRepository;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceListingRepository serviceListingRepository;
    private final BookingMapper bookingMapper;
    private final ServiceListingMapper serviceListingMapper;

    private User getUserOrThrow(String userEmail) {
        String email = (userEmail != null && !userEmail.isBlank()) ? userEmail : "guest@localconnect.com";
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    if (email.contains("guest")) {
                        return userRepository.save(User.builder()
                                .email(email)
                                .fullName("Guest User")
                                .password("guest123")
                                .role(RoleName.USER)
                                .enabled(true)
                                .createdAt(LocalDateTime.now())
                                .build());
                    }
                    throw new RuntimeException("User not found");
                });
    }

    @Override
    public BookingResponse createBooking(BookingCreateRequest request, String userEmail) {
        User user = getUserOrThrow(userEmail);

        ServiceListing serviceListing = serviceListingRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (serviceListing.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("Service is not approved");
        }

        LocalDateTime bookingDate = request.getBookingDate() != null
                ? request.getBookingDate()
                : LocalDateTime.now().plusDays(1);

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
        User user = getUserOrThrow(userEmail);

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

    @Override
    public BookingResponse cancelBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse rescheduleBooking(Long bookingId, String newBookingDateStr, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        LocalDateTime newDate;
        try {
            newDate = LocalDateTime.parse(newBookingDateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                newDate = LocalDateTime.parse(newBookingDateStr);
            } catch (Exception ex) {
                newDate = LocalDateTime.now().plusDays(1);
            }
        }

        booking.setBookingDate(newDate);
        booking.setStatus(BookingStatus.PENDING);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse getBookingStatus(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        return bookingMapper.toResponse(booking);
    }

    @Override
    public List<ServiceListingResponse> recommendServices(String category, String city) {
        List<ServiceListing> listings;
        if (category != null && !category.isBlank() && city != null && !city.isBlank()) {
            listings = serviceListingRepository.findByCategoryContainingIgnoreCaseAndApprovalStatus(category, ApprovalStatus.APPROVED)
                    .stream()
                    .filter(s -> s.getCity() != null && s.getCity().equalsIgnoreCase(city))
                    .toList();
        } else if (category != null && !category.isBlank()) {
            listings = serviceListingRepository.findByCategoryContainingIgnoreCaseAndApprovalStatus(category, ApprovalStatus.APPROVED);
            if (listings.isEmpty()) {
                listings = serviceListingRepository.findByApprovalStatus(ApprovalStatus.APPROVED)
                        .stream()
                        .filter(s -> (s.getTitle() != null && s.getTitle().toLowerCase().contains(category.toLowerCase())) ||
                                     (s.getSubCategory() != null && s.getSubCategory().toLowerCase().contains(category.toLowerCase())))
                        .toList();
            }
        } else if (city != null && !city.isBlank()) {
            listings = serviceListingRepository.findByCityContainingIgnoreCaseAndApprovalStatus(city, ApprovalStatus.APPROVED);
        } else {
            listings = serviceListingRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        }

        if (listings.isEmpty()) {
            listings = serviceListingRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        }

        return listings.stream()
                .map(serviceListingMapper::toResponse)
                .toList();
    }
}