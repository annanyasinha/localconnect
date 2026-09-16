package com.localconnect.backend.controller;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingResponse createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            Principal principal) {

        return bookingService.createBooking(
                request,
                principal.getName()
        );
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(
            Principal principal) {

        return bookingService.getMyBookings(
                principal.getName()
        );
    }

    @GetMapping("/provider")
    public List<BookingResponse> providerBookings(
            Principal principal) {

        return bookingService.getProviderBookings(
                principal.getName()
        );
    }

    @PutMapping("/{id}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable Long id,
            Principal principal) {

        return bookingService.cancelBooking(
                id,
                principal.getName()
        );
    }

    @PutMapping("/{id}/reschedule")
    public BookingResponse rescheduleBooking(
            @PathVariable Long id,
            @RequestParam String newBookingDate,
            Principal principal) {

        return bookingService.rescheduleBooking(
                id,
                newBookingDate,
                principal.getName()
        );
    }

    @GetMapping("/{id}")
    public BookingResponse getBookingStatus(
            @PathVariable Long id,
            Principal principal) {

        return bookingService.getBookingStatus(
                id,
                principal.getName()
        );
    }
}