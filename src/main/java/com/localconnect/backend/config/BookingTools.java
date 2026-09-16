package com.localconnect.backend.config;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

@Configuration
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingTools {

    private final BookingService bookingService;

    // Record classes for Tool Request inputs
    public record BookServiceRequest(Long serviceId, String bookingDate, String message) {}
    public record CancelBookingRequest(Long bookingId) {}
    public record RescheduleBookingRequest(Long bookingId, String newBookingDate) {}
    public record CheckStatusRequest(Long bookingId) {}
    public record RecommendServicesRequest(String category, String city) {}

    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
    }

    @Tool(description = "Book a local service for a given service ID, date, and optional message")
    public BookingResponse bookService(Long serviceId, String bookingDate, String message) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("AI @Tool Executed: bookService for serviceId={}, user={}", serviceId, userEmail);

        if (bookingDate == null || bookingDate.isBlank()) {
            throw new IllegalArgumentException("Invalid booking date. Please provide a valid date and time.");
        }

        BookingCreateRequest dto = new BookingCreateRequest();
        dto.setServiceId(serviceId);
        dto.setMessage(message != null ? message : "Booked via AI Tool");

        try {
            dto.setBookingDate(LocalDateTime.parse(bookingDate, DateTimeFormatter.ISO_DATE_TIME));
        } catch (DateTimeParseException e) {
            try {
                dto.setBookingDate(LocalDateTime.parse(bookingDate));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid booking date. Please provide a valid date and time.");
            }
        }

        return bookingService.createBooking(dto, userEmail);
    }

    @Tool(description = "Cancel an existing booking using booking ID")
    public BookingResponse cancelBooking(Long bookingId) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("AI @Tool Executed: cancelBooking for bookingId={}, user={}", bookingId, userEmail);
        return bookingService.cancelBooking(bookingId, userEmail);
    }

    @Tool(description = "Reschedule an existing booking with a new date/time")
    public BookingResponse rescheduleBooking(Long bookingId, String newBookingDate) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("AI @Tool Executed: rescheduleBooking for bookingId={}, newDate={}", bookingId, newBookingDate);
        return bookingService.rescheduleBooking(bookingId, newBookingDate, userEmail);
    }

    @Tool(description = "Check the current status of a booking by booking ID")
    public BookingResponse checkBookingStatus(Long bookingId) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("AI @Tool Executed: checkBookingStatus for bookingId={}", bookingId);
        return bookingService.getBookingStatus(bookingId, userEmail);
    }

    @Tool(description = "Search and recommend available local services by category and city")
    public List<ServiceListingResponse> recommendServices(String category, String city) {
        log.info("AI @Tool Executed: recommendServices for category={}, city={}", category, city);
        return bookingService.recommendServices(category, city);
    }

    @Bean
    @Description("Book a local service for a given service ID, date, and optional message")
    public Function<BookServiceRequest, BookingResponse> bookServiceFunction() {
        return request -> bookService(request.serviceId(), request.bookingDate(), request.message());
    }

    @Bean
    @Description("Cancel an existing booking using booking ID")
    public Function<CancelBookingRequest, BookingResponse> cancelBookingFunction() {
        return request -> cancelBooking(request.bookingId());
    }

    @Bean
    @Description("Reschedule an existing booking with a new date/time")
    public Function<RescheduleBookingRequest, BookingResponse> rescheduleBookingFunction() {
        return request -> rescheduleBooking(request.bookingId(), request.newBookingDate());
    }

    @Bean
    @Description("Check the current status of a booking by booking ID")
    public Function<CheckStatusRequest, BookingResponse> checkBookingStatusFunction() {
        return request -> checkBookingStatus(request.bookingId());
    }

    @Bean
    @Description("Search and recommend available local services by category and city")
    public Function<RecommendServicesRequest, List<ServiceListingResponse>> recommendServicesFunction() {
        return request -> recommendServices(request.category(), request.city());
    }
}
