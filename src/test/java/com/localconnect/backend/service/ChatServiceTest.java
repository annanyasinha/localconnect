package com.localconnect.backend.service;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ChatResponse;
import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.entity.ChatMessage;
import com.localconnect.backend.enums.BookingStatus;
import com.localconnect.backend.repository.ChatMessageRepository;
import com.localconnect.backend.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private BookingResponse mockBookingResponse;

    @BeforeEach
    void setUp() {
        mockBookingResponse = new BookingResponse();
        mockBookingResponse.setId(1L);
        mockBookingResponse.setServiceId(10L);
        mockBookingResponse.setServiceTitle("Plumbing Repair");
        mockBookingResponse.setBookingDate(LocalDateTime.now().plusDays(1));
        mockBookingResponse.setStatus(BookingStatus.PENDING.name());
    }

    @Test
    void testProcessChatMessage_BookService() {
        when(bookingService.createBooking(any(BookingCreateRequest.class), anyString()))
                .thenReturn(mockBookingResponse);

        ChatRequest request = ChatRequest.builder()
                .message("Book service #10 for tomorrow")
                .conversationId("test-conv-123")
                .userEmail("test@example.com")
                .build();

        ChatResponse response = chatService.processChatMessage(request);

        assertNotNull(response);
        assertEquals("BOOKING_CREATED", response.getActionPerformed());
        assertTrue(response.getReply().contains("Plumbing Repair") || response.getReply().contains("#1"));
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void testProcessChatMessage_CancelBooking() {
        BookingResponse cancelledRes = new BookingResponse();
        cancelledRes.setId(1L);
        cancelledRes.setServiceTitle("Plumbing Repair");
        cancelledRes.setStatus(BookingStatus.CANCELLED.name());

        when(bookingService.cancelBooking(eq(1L), anyString()))
                .thenReturn(cancelledRes);

        ChatRequest request = ChatRequest.builder()
                .message("Cancel booking #1")
                .conversationId("test-conv-123")
                .userEmail("test@example.com")
                .build();

        ChatResponse response = chatService.processChatMessage(request);

        assertNotNull(response);
        assertEquals("BOOKING_CANCELLED", response.getActionPerformed());
        assertTrue(response.getReply().contains("cancelled"));
    }

    @Test
    void testProcessChatMessage_RecommendServices() {
        ServiceListingResponse s1 = new ServiceListingResponse();
        s1.setId(10L);
        s1.setTitle("Expert Plumbing");
        s1.setCategory("Plumber");
        s1.setPrice(new java.math.BigDecimal("500.00"));
        s1.setCity("Jamshedpur");

        when(bookingService.recommendServices(anyString(), anyString()))
                .thenReturn(List.of(s1));

        ChatRequest request = ChatRequest.builder()
                .message("Recommend services in Jamshedpur")
                .conversationId("test-conv-123")
                .userEmail("test@example.com")
                .build();

        ChatResponse response = chatService.processChatMessage(request);

        assertNotNull(response);
        assertTrue(response.getReply().contains("Expert Plumbing") || response.getReply().contains("Top Services") || response.getReply().contains("LocalConnect AI"));
    }
}
