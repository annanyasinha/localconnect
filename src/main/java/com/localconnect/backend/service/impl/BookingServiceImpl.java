package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.entity.Booking;
import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.ApprovalStatus;
import com.localconnect.backend.enums.BookingStatus;
import com.localconnect.backend.mapper.BookingMapper;
import com.localconnect.backend.mapper.ServiceListingMapper;
import com.localconnect.backend.repository.BookingRepository;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Please log in"
                ));
    }

    private void verifyOwnership(Booking booking, String userEmail) {
        boolean isCustomer = booking.getUser() != null &&
                             booking.getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isProvider = booking.getServiceListing() != null &&
                             booking.getServiceListing().getProvider() != null &&
                             booking.getServiceListing().getProvider().getEmail().equalsIgnoreCase(userEmail);

        if (!isCustomer && !isProvider) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, String userEmail) {
        User user = getUserOrThrow(userEmail);

        ServiceListing serviceListing = serviceListingRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Service not found"
                ));

        if (serviceListing.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Service is not approved"
            );
        }

        if (!Boolean.TRUE.equals(serviceListing.getAvailable())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Service is currently unavailable"
            );
        }

        LocalDateTime bookingDate = request.getBookingDate();
        if (bookingDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date is required"
            );
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (!bookingDate.isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date must be in the future"
            );
        }

        boolean doubleBooked = bookingRepository.existsByServiceListingAndBookingDateAndStatusNot(
                serviceListing, bookingDate, BookingStatus.CANCELLED
        );
        if (doubleBooked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This time slot is already booked for this service"
            );
        }

        Booking booking = Booking.builder()
                .user(user)
                .serviceListing(serviceListing)
                .message(request.getMessage())
                .bookingDate(bookingDate)
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Provider not found"
                ));

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
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        verifyOwnership(booking, userEmail);

        if (booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This booking cannot be cancelled"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse rescheduleBooking(Long bookingId, String newBookingDateStr, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        verifyOwnership(booking, userEmail);

        if (booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This booking cannot be rescheduled"
            );
        }

        LocalDateTime newDate;
        try {
            newDate = LocalDateTime.parse(newBookingDateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                newDate = LocalDateTime.parse(newBookingDateStr);
            } catch (Exception ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid booking date format"
                );
            }
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (!newDate.isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date must be in the future"
            );
        }

        boolean doubleBooked = bookingRepository.existsByServiceListingAndBookingDateAndStatusNot(
                booking.getServiceListing(), newDate, BookingStatus.CANCELLED
        );
        if (doubleBooked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This time slot is already booked for this service"
            );
        }

        booking.setBookingDate(newDate);
        booking.setStatus(BookingStatus.PENDING);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse getBookingStatus(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        verifyOwnership(booking, userEmail);
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