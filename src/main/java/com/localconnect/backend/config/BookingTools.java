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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@Configuration
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingTools {

    private final BookingService bookingService;

    // Record classes for Tool Request inputs
    public record BookServiceRequest(Long serviceId, String userEmail, String bookingDate, String message) {}
    public record CancelBookingRequest(Long bookingId, String userEmail) {}
    public record RescheduleBookingRequest(Long bookingId, String newBookingDate, String userEmail) {}
    public record CheckStatusRequest(Long bookingId, String userEmail) {}
    public record RecommendServicesRequest(String category, String city) {}

    @Tool(description = "Book a local service or ticket for a given service ID, date, and user email")
    public BookingResponse bookService(Long serviceId, String userEmail, String bookingDate, String message) {
        log.info("AI @Tool Executed: bookService for serviceId={}, user={}", serviceId, userEmail);
        BookingCreateRequest dto = new BookingCreateRequest();
        dto.setServiceId(serviceId);
        dto.setMessage(message != null ? message : "Booked via AI Tool");
        if (bookingDate != null && !bookingDate.isBlank()) {
            try {
                dto.setBookingDate(LocalDateTime.parse(bookingDate, DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) {
                dto.setBookingDate(LocalDateTime.now().plusDays(1));
            }
        } else {
            dto.setBookingDate(LocalDateTime.now().plusDays(1));
        }
        return bookingService.createBooking(dto, userEmail);
    }

    @Tool(description = "Cancel an existing booking using booking ID and user email")
    public BookingResponse cancelBooking(Long bookingId, String userEmail) {
        log.info("AI @Tool Executed: cancelBooking for bookingId={}, user={}", bookingId, userEmail);
        return bookingService.cancelBooking(bookingId, userEmail);
    }

    @Tool(description = "Reschedule an existing booking with a new date/time")
    public BookingResponse rescheduleBooking(Long bookingId, String newBookingDate, String userEmail) {
        log.info("AI @Tool Executed: rescheduleBooking for bookingId={}, newDate={}", bookingId, newBookingDate);
        return bookingService.rescheduleBooking(bookingId, newBookingDate, userEmail);
    }

    @Tool(description = "Check the current status of a booking by booking ID and user email")
    public BookingResponse checkBookingStatus(Long bookingId, String userEmail) {
        log.info("AI @Tool Executed: checkBookingStatus for bookingId={}", bookingId);
        return bookingService.getBookingStatus(bookingId, userEmail);
    }

    @Tool(description = "Search and recommend available local services by category and city")
    public List<ServiceListingResponse> recommendServices(String category, String city) {
        log.info("AI @Tool Executed: recommendServices for category={}, city={}", category, city);
        return bookingService.recommendServices(category, city);
    }

    @Bean
    @Description("Book a local service or ticket for a given service ID, date, and user email")
    public Function<BookServiceRequest, BookingResponse> bookServiceFunction() {
        return request -> bookService(request.serviceId(), request.userEmail(), request.bookingDate(), request.message());
    }

    @Bean
    @Description("Cancel an existing booking using booking ID and user email")
    public Function<CancelBookingRequest, BookingResponse> cancelBookingFunction() {
        return request -> cancelBooking(request.bookingId(), request.userEmail());
    }

    @Bean
    @Description("Reschedule an existing booking with a new date/time")
    public Function<RescheduleBookingRequest, BookingResponse> rescheduleBookingFunction() {
        return request -> rescheduleBooking(request.bookingId(), request.newBookingDate(), request.userEmail());
    }

    @Bean
    @Description("Check the current status of a booking by booking ID and user email")
    public Function<CheckStatusRequest, BookingResponse> checkBookingStatusFunction() {
        return request -> checkBookingStatus(request.bookingId(), request.userEmail());
    }

    @Bean
    @Description("Search and recommend available local services by category and city")
    public Function<RecommendServicesRequest, List<ServiceListingResponse>> recommendServicesFunction() {
        return request -> recommendServices(request.category(), request.city());
    }
}
