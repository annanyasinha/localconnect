package com.localconnect.backend.controller;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.service.BookingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    private String authenticatedEmail(Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        return principal.getName();
    }

    @PostMapping
    public BookingResponse createBooking(
            @RequestBody BookingCreateRequest request,
            Principal principal) {

        return bookingService.createBooking(
                request,
                authenticatedEmail(principal));
    }

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings(
            Principal principal) {

        return bookingService.getMyBookings(
                authenticatedEmail(principal));
    }

    @GetMapping("/provider")
    public List<BookingResponse> getProviderBookings(
            Principal principal) {

        return bookingService.getProviderBookings(
                authenticatedEmail(principal));
    }
}