package com.localconnect.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ServiceListingResponse;

import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.enums.ApprovalStatus;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.service.BookingService;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class BookingTools {

    private final BookingService bookingService;
    private final ServiceListingRepository serviceRepository;

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    // A proposal is NOT a real booking.
    private record PendingBooking(
            Long serviceId,
            LocalDateTime bookingDate,
            String summary) {
    }

    // Temporary booking proposals expire after 15 minutes.
    private final Cache<String, PendingBooking> pendingBookings = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(10000)
            .build();

    private String key(
            String email,
            String conversationId) {
        return email.toLowerCase(Locale.ROOT)
                + ":" + conversationId;
    }

    // ==========================================
    // AUTHENTICATED TOOL CONTEXT
    // ==========================================

    private String contextValue(
            ToolContext context,
            String name) {

        Object value = context.getContext().get(name);

        if (!(value instanceof String text)
                || text.isBlank()) {

            throw new SecurityException(
                    "Missing authenticated context: " + name);
        }

        return text;
    }

    // ==========================================
    // TOOL 1: CURRENT TIME
    // ==========================================

    @Tool(description = "Get the current date and time in India. Use this to understand tomorrow and other relative dates.")
    public String currentIndiaTime() {

        return LocalDateTime.now(INDIA).toString();
    }

    // ==========================================
    // TOOL 2: SEARCH REAL SERVICES
    // ==========================================

    @Tool(description = "Search real approved and available local services by category and city. Use empty strings for missing filters. Never invent service IDs or prices.")
    public List<ServiceListingResponse> recommendServices(

            @ToolParam(description = "Category such as Plumber, Tutor, Cook or Electrician; empty string means all.") String category,

            @ToolParam(description = "City; empty string means all.") String city) {

        return bookingService.recommendServices(
                category == null ? "" : category,
                city == null ? "" : city).stream().limit(8).toList();
    }

    // ==========================================
    // TOOL 3: SHOW MY BOOKINGS
    // ==========================================

    @Tool(description = "Show bookings belonging to the authenticated customer.")
    public List<BookingResponse> myBookings(
            ToolContext context) {

        String email = contextValue(
                context,
                "userEmail");

        return bookingService.getMyBookings(email);
    }

    // ==========================================
    // TOOL 4: CHECK BOOKING STATUS
    // ==========================================

    @Tool(description = "Check the status of an existing booking belonging to the authenticated customer.")
    public BookingResponse checkBookingStatus(

            @ToolParam(description = "Existing booking ID") Long bookingId,

            ToolContext context) {

        String email = contextValue(
                context,
                "userEmail");

        return bookingService.getBookingStatus(
                bookingId,
                email);
    }

    // ==========================================
    // TOOL 5: PREPARE BOOKING
    // DOES NOT CREATE A REAL BOOKING
    // ==========================================

    @Tool(description = """
            Prepare a booking proposal without creating a real booking.

            Call only when the user has selected a real service ID
            and supplied an explicit date and time.

            Use yyyy-MM-ddTHH:mm format in Asia/Kolkata.

            If any information is missing, ask the user.
            Never guess a service ID or booking date.

            The user must confirm in a separate message.
            """)
    public String proposeBooking(

            @ToolParam(description = "Real service ID selected by the user.") Long serviceId,

            @ToolParam(description = "Future date and time in yyyy-MM-ddTHH:mm format.") String bookingDate,

            ToolContext context) {

        String email = contextValue(
                context,
                "userEmail");

        String conversationId = contextValue(
                context,
                "conversationId");

        if (serviceId == null || serviceId <= 0) {

            throw new IllegalArgumentException(
                    "Please select a valid service.");
        }

        LocalDateTime date;

        try {

            date = LocalDateTime.parse(bookingDate);

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Please provide a valid date and time.");
        }

        if (!date.isAfter(LocalDateTime.now(INDIA))) {

            throw new IllegalArgumentException(
                    "Booking date must be in the future.");
        }

        ServiceListing service = serviceRepository.findById(serviceId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Service does not exist."));

        if (service.getApprovalStatus() != ApprovalStatus.APPROVED
                || !Boolean.TRUE.equals(
                        service.getAvailable())) {

            throw new IllegalArgumentException(
                    "This service is currently unavailable.");
        }

        String summary = "Please confirm your booking:\n\n"
                + "Service: " + service.getTitle()
                + "\nService ID: #" + service.getId()
                + "\nPrice: ₹" + service.getPrice()
                + "\nCity: " + service.getCity()
                + "\nDate and time: " + date
                + "\n\nReply 'confirm booking' to proceed."
                + "\nOr reply 'cancel request'.";

        pendingBookings.put(
                key(email, conversationId),
                new PendingBooking(
                        serviceId,
                        date,
                        summary));

        Object signal = context.getContext()
                .get("proposalStaged");

        if (signal instanceof AtomicBoolean flag) {

            flag.set(true);
        }

        return summary;
    }

    // ==========================================
    // GET PENDING BOOKING SUMMARY
    // ==========================================

    public String getPendingSummary(
            String email,
            String conversationId) {

        PendingBooking pending = pendingBookings.getIfPresent(
                key(email, conversationId));

        return pending == null
                ? null
                : pending.summary();
    }

    // ==========================================
    // CONFIRM BOOKING
    // NOT EXPOSED AS AN AI TOOL
    // ==========================================

    public synchronized BookingResponse confirmBooking(
            String email,
            String conversationId) {

        String bookingKey = key(email, conversationId);

        PendingBooking pending = pendingBookings.getIfPresent(bookingKey);

        if (pending == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No booking is waiting for confirmation.");
        }

        // Remove the proposal before creating a booking
        // to prevent duplicate confirmations on this instance.
        pendingBookings.invalidate(bookingKey);

        BookingCreateRequest request = new BookingCreateRequest();

        request.setServiceId(
                pending.serviceId());

        request.setBookingDate(
                pending.bookingDate());

        request.setMessage(
                "Confirmed through LocalConnect AI");

        // BookingService must revalidate service availability.
        return bookingService.createBooking(
                request,
                email);
    }

    // ==========================================
    // CANCEL UNCONFIRMED BOOKING REQUEST
    // ==========================================

    public void cancelRequest(
            String email,
            String conversationId) {

        pendingBookings.invalidate(
                key(email, conversationId));
    }
}