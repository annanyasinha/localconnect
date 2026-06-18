package com.localconnect.backend.controller;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingResponse createBooking(@RequestBody BookingCreateRequest request,
                                         @RequestParam String userEmail) {
        return bookingService.createBooking(request, userEmail);
    }

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings(@RequestParam String userEmail) {
        return bookingService.getMyBookings(userEmail);
    }

    @GetMapping("/provider")
    public List<BookingResponse> getProviderBookings(@RequestParam String providerEmail) {
        return bookingService.getProviderBookings(providerEmail);
    }
}