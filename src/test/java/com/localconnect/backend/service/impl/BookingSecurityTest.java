package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.BookingCreateRequest;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSecurityTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceListingRepository serviceListingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private ServiceListingMapper serviceListingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Booking bookingOwnedBy(String email) {

        User owner = User.builder()
                .email(email)
                .build();

        return Booking.builder()
                .id(5L)
                .user(owner)
                .status(BookingStatus.PENDING)
                .build();
    }

    @Test
    void shouldRejectCancellationByAnotherUser() {

        Booking booking = bookingOwnedBy("owner@example.com");

        when(
                bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelBooking(
                        5L,
                        "attacker@example.com"));

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode());

        verify(
                bookingRepository,
                never()).save(any());
    }

    @Test
    void shouldRejectStatusLookupByAnotherUser() {

        Booking booking = bookingOwnedBy("owner@example.com");

        when(
                bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingStatus(
                        5L,
                        "attacker@example.com"));

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode());

        verifyNoInteractions(bookingMapper);
    }

    @Test
    void shouldRejectReschedulingByAnotherUser() {

        Booking booking = bookingOwnedBy("owner@example.com");

        when(
                bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.rescheduleBooking(
                        5L,
                        "2030-01-01T10:00",
                        "attacker@example.com"));

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode());

        verify(
                bookingRepository,
                never()).save(any());
    }

    @Test
    void shouldRejectUnavailableService() {

        User user = User.builder()
                .email("customer@example.com")
                .build();

        ServiceListing listing = ServiceListing.builder()
                .id(10L)
                .approvalStatus(
                        ApprovalStatus.APPROVED)
                .available(false)
                .build();

        BookingCreateRequest request = new BookingCreateRequest();

        request.setServiceId(10L);

        when(
                userRepository.findByEmail(
                        "customer@example.com"))
                .thenReturn(Optional.of(user));

        when(
                serviceListingRepository.findById(10L)).thenReturn(Optional.of(listing));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookingService.createBooking(
                        request,
                        "customer@example.com"));

        assertEquals(
                "This service is currently unavailable",
                exception.getMessage());

        verify(
                bookingRepository,
                never()).save(any());
    }
}