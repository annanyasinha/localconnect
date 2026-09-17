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

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

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

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    // ------------------------------------------
    // GET AUTHENTICATED USER
    // ------------------------------------------

    private User getUserOrThrow(String userEmail) {

        if (userEmail == null || userEmail.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        return userRepository.findByEmail(userEmail)
                .orElseThrow(
                        () -> new RuntimeException("User not found"));
    }

    // ------------------------------------------
    // VERIFY BOOKING OWNERSHIP
    // ------------------------------------------

    private void verifyBookingOwner(
            Booking booking,
            String userEmail) {

        if (userEmail == null
                || booking.getUser() == null
                || booking.getUser().getEmail() == null
                || !userEmail.equalsIgnoreCase(
                        booking.getUser().getEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another user's booking");
        }
    }

    // ------------------------------------------
    // CREATE BOOKING
    // ------------------------------------------

    @Override
    public BookingResponse createBooking(
            BookingCreateRequest request,
            String userEmail) {

        User user = getUserOrThrow(userEmail);

        if (request == null || request.getServiceId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Service ID is required");
        }

        ServiceListing serviceListing = serviceListingRepository.findById(
                request.getServiceId())
                .orElseThrow(
                        () -> new RuntimeException(
                                "Service not found"));

        if (serviceListing.getApprovalStatus() != ApprovalStatus.APPROVED) {

            throw new RuntimeException(
                    "Service is not approved");
        }

        if (Boolean.FALSE.equals(serviceListing.getAvailable())) {

            throw new RuntimeException(
                    "This service is currently unavailable");
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        LocalDateTime bookingDate = request.getBookingDate() != null
                ? request.getBookingDate()
                : now.plusDays(1);

        if (!bookingDate.isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date must be in the future");
        }

        Booking booking = Booking.builder()
                .user(user)
                .serviceListing(serviceListing)
                .message(request.getMessage())
                .bookingDate(bookingDate)
                .status(BookingStatus.PENDING)
                .createdAt(now)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        return bookingMapper.toResponse(savedBooking);
    }

    // ------------------------------------------
    // GET CUSTOMER BOOKINGS
    // ------------------------------------------

    @Override
    public List<BookingResponse> getMyBookings(
            String userEmail) {

        User user = getUserOrThrow(userEmail);

        return bookingRepository.findByUser(user)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    // ------------------------------------------
    // GET PROVIDER BOOKINGS
    // ------------------------------------------

    @Override
    public List<BookingResponse> getProviderBookings(
            String providerEmail) {

        User provider = getUserOrThrow(providerEmail);

        if (provider.getRole() != RoleName.PROVIDER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only providers can view provider bookings");
        }

        List<ServiceListing> myServices = serviceListingRepository.findByProvider(provider);

        List<Booking> allBookings = new ArrayList<>();

        for (ServiceListing service : myServices) {

            allBookings.addAll(
                    bookingRepository.findByServiceListing(service));
        }

        return allBookings.stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    // ------------------------------------------
    // CANCEL BOOKING
    // ------------------------------------------

    @Override
    public BookingResponse cancelBooking(
            Long bookingId,
            String userEmail) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Booking not found with ID: " + bookingId));

        verifyBookingOwner(booking, userEmail);

        if (booking.getStatus() == BookingStatus.CANCELLED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Completed bookings cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Booking savedBooking = bookingRepository.save(booking);

        return bookingMapper.toResponse(savedBooking);
    }

    // ------------------------------------------
    // RESCHEDULE BOOKING
    // ------------------------------------------

    @Override
    public BookingResponse rescheduleBooking(
            Long bookingId,
            String newBookingDateStr,
            String userEmail) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Booking not found with ID: " + bookingId));

        verifyBookingOwner(booking, userEmail);

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This booking cannot be rescheduled");
        }

        LocalDateTime newDate;

        try {

            newDate = LocalDateTime.parse(
                    newBookingDateStr);

        } catch (
                DateTimeParseException | NullPointerException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date. Use yyyy-MM-ddTHH:mm");
        }

        if (!newDate.isAfter(LocalDateTime.now(APP_ZONE))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date must be in the future");
        }

        booking.setBookingDate(newDate);

        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        return bookingMapper.toResponse(savedBooking);
    }

    // ------------------------------------------
    // CHECK BOOKING STATUS
    // ------------------------------------------

    @Override
    public BookingResponse getBookingStatus(
            Long bookingId,
            String userEmail) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Booking not found with ID: " + bookingId));

        verifyBookingOwner(booking, userEmail);

        return bookingMapper.toResponse(booking);
    }

    // ------------------------------------------
    // CACHED SERVICE RECOMMENDATIONS
    // ------------------------------------------

    @Override
    @Cacheable(cacheNames = "serviceRecommendations")
    public List<ServiceListingResponse> recommendServices(
            String category,
            String city) {

        String searchCategory = category == null ? "" : category.trim();

        String searchCity = city == null ? "" : city.trim();

        List<ServiceListing> listings;

        // CASE 1: Category provided
        if (!searchCategory.isBlank()) {

            listings = serviceListingRepository
                    .findByCategoryContainingIgnoreCaseAndApprovalStatus(
                            searchCategory,
                            ApprovalStatus.APPROVED);

            // Search title and subcategory if category has no matches
            if (listings.isEmpty()) {

                String keyword = searchCategory.toLowerCase(
                        java.util.Locale.ROOT);

                listings = serviceListingRepository
                        .findByApprovalStatusAndAvailableTrue(
                                ApprovalStatus.APPROVED)
                        .stream()
                        .filter(service -> {

                            boolean titleMatches = service.getTitle() != null
                                    && service.getTitle()
                                            .toLowerCase(
                                                    java.util.Locale.ROOT)
                                            .contains(keyword);

                            boolean subCategoryMatches = service.getSubCategory() != null
                                    && service.getSubCategory()
                                            .toLowerCase(
                                                    java.util.Locale.ROOT)
                                            .contains(keyword);

                            return titleMatches || subCategoryMatches;
                        })
                        .toList();
            }

            // CASE 2: Only city provided
        } else if (!searchCity.isBlank()) {

            listings = serviceListingRepository
                    .findByCityContainingIgnoreCaseAndApprovalStatus(
                            searchCity,
                            ApprovalStatus.APPROVED);

            // CASE 3: No filters
        } else {

            listings = serviceListingRepository
                    .findByApprovalStatusAndAvailableTrue(
                            ApprovalStatus.APPROVED);
        }

        // Final availability and city filtering
        return listings.stream()
                .filter(service -> Boolean.TRUE.equals(
                        service.getAvailable()))
                .filter(service -> searchCity.isBlank()
                        || (service.getCity() != null
                                && service.getCity()
                                        .equalsIgnoreCase(searchCity)))
                .map(serviceListingMapper::toResponse)
                .toList();
    }
}