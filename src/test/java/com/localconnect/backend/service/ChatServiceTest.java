package com.localconnect.backend.service;

import com.localconnect.backend.config.BookingTools;
import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ChatResponse;
import com.localconnect.backend.entity.ChatMessage;
import com.localconnect.backend.repository.ChatMessageRepository;
import com.localconnect.backend.service.impl.ChatServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        private BookingTools bookingTools;

        @Mock
        private ChatModel chatModel;

        @InjectMocks
        private ChatServiceImpl chatService;

        private ChatRequest request(String message) {
                return ChatRequest.builder()
                                .message(message)
                                .conversationId("test-conversation")
                                .userEmail("test@example.com")
                                .build();
        }

        private void mockAIResponse(String message) {

                org.springframework.ai.chat.model.ChatResponse aiResponse = new org.springframework.ai.chat.model.ChatResponse(
                                List.of(
                                                new Generation(
                                                                new AssistantMessage(message))));

                when(chatModel.call(any(Prompt.class)))
                                .thenReturn(aiResponse);
        }

        @Test
        void shouldReturnAIResponse() {

                mockAIResponse("Hello! How can I help you?");

                ChatResponse response = chatService.processChatMessage(
                                request("Hello"));

                assertEquals(
                                "Hello! How can I help you?",
                                response.getReply());

                assertEquals("CHAT", response.getActionPerformed());

                assertEquals(
                                "test-conversation",
                                response.getConversationId());

                verify(chatModel).call(any(Prompt.class));

                verify(bookingTools, never())
                                .confirmBooking(anyString(), anyString());
        }

        @Test
        void shouldSaveUserAndAssistantMessages() {

                mockAIResponse("Hello!");

                chatService.processChatMessage(
                                request("Hi"));

                ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);

                verify(chatMessageRepository, times(2))
                                .save(captor.capture());

                List<ChatMessage> saved = captor.getAllValues();
                assertEquals("USER", saved.get(0).getSender());
                assertEquals("Hi", saved.get(0).getContent());

                assertEquals("ASSISTANT", saved.get(1).getSender());
                assertEquals("Hello!", saved.get(1).getContent());

                assertEquals(
                                "test@example.com",
                                saved.get(0).getUserEmail());
        }

        @Test
        void shouldAskForTimeWhenMissing() {

                mockAIResponse(
                                "What date and time would you like to book?");

                ChatResponse response = chatService.processChatMessage(
                                request("Book service #10 tomorrow"));

                assertEquals("CHAT", response.getActionPerformed());

                assertTrue(
                                response.getReply().contains("What date and time"));

                verify(bookingTools, never())
                                .confirmBooking(anyString(), anyString());
        }

        @Test
        void shouldNotCreateBookingWithoutConfirmation() {

                mockAIResponse(
                                "Please provide the booking details first.");

                ChatResponse response = chatService.processChatMessage(
                                request("Book service #10 tomorrow at 5 PM"));

                assertEquals("CHAT", response.getActionPerformed());

                verify(bookingTools, never())
                                .confirmBooking(anyString(), anyString());
        }

        @Test
        void shouldCreateBookingAfterExplicitConfirmation() {

                BookingResponse booking = new BookingResponse();

                booking.setId(1L);
                booking.setServiceId(10L);
                booking.setServiceTitle("Plumbing Repair");
                booking.setBookingDate(
                                LocalDateTime.now().plusDays(1));
                booking.setStatus("PENDING");

                when(
                                bookingTools.confirmBooking(
                                                "test@example.com",
                                                "test-conversation"))
                                .thenReturn(booking);

                ChatResponse response = chatService.processChatMessage(
                                request("confirm booking"));

                assertEquals(
                                "BOOKING_CREATED",
                                response.getActionPerformed());

                assertTrue(
                                response.getReply().contains("Booking ID: #1"));

                verify(bookingTools).confirmBooking(
                                "test@example.com",
                                "test-conversation");

                verifyNoInteractions(chatModel);
        }

        @Test
        void shouldHandleConfirmationWithoutPendingBooking() {

                when(
                                bookingTools.confirmBooking(
                                                "test@example.com",
                                                "test-conversation"))
                                .thenThrow(
                                                new ResponseStatusException(
                                                                HttpStatus.BAD_REQUEST,
                                                                "No pending booking."));

                ChatResponse response = chatService.processChatMessage(
                                request("confirm booking"));

                assertEquals("CHAT", response.getActionPerformed());

                assertTrue(
                                response.getReply().contains("No pending booking"));

                verifyNoInteractions(chatModel);
        }

        @Test
        void shouldDiscardUnconfirmedBookingRequest() {

                ChatResponse response = chatService.processChatMessage(
                                request("cancel request"));

                assertEquals("CHAT", response.getActionPerformed());

                assertTrue(
                                response.getReply().contains(
                                                "Booking request discarded"));

                verify(bookingTools).cancelRequest(
                                "test@example.com",
                                "test-conversation");

                verify(bookingTools, never())
                                .confirmBooking(anyString(), anyString());

                verifyNoInteractions(chatModel);
        }

        @Test
        void shouldRejectAnotherUsersConversation() {

                ChatMessage existingMessage = ChatMessage.builder()
                                .conversationId("test-conversation")
                                .userEmail("victim@example.com")
                                .sender("USER")
                                .content("Private message")
                                .createdAt(LocalDateTime.now())
                                .build();

                when(
                                chatMessageRepository
                                                .findByConversationIdOrderByCreatedAtAsc(
                                                                "test-conversation"))
                                .thenReturn(List.of(existingMessage));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> chatService.processChatMessage(
                                                request("Hello")));

                assertEquals(
                                HttpStatus.FORBIDDEN,
                                exception.getStatusCode());

                verify(chatMessageRepository, never())
                                .save(any(ChatMessage.class));

                verifyNoInteractions(chatModel);
        }

        @Test
        void shouldRejectMissingAuthenticatedEmail() {

                ChatRequest invalidRequest = ChatRequest.builder()
                                .message("Hello")
                                .conversationId("test-conversation")
                                .build();

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> chatService.processChatMessage(
                                                invalidRequest));

                assertEquals(
                                HttpStatus.UNAUTHORIZED,
                                exception.getStatusCode());

                verifyNoInteractions(chatModel, bookingTools);
        }

        @Test
        void shouldRejectEmptyMessage() {

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> chatService.processChatMessage(
                                                request("   ")));

                assertEquals(
                                HttpStatus.BAD_REQUEST,
                                exception.getStatusCode());

                verifyNoInteractions(chatModel);
        }

        @Test
        void shouldReturn503WhenAIResponseIsEmpty() {

                mockAIResponse("");

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> chatService.processChatMessage(
                                                request("Hello")));

                assertEquals(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                exception.getStatusCode());

                verify(bookingTools, never())
                                .confirmBooking(anyString(), anyString());
        }

        @Test
        void shouldReturn503WhenAIModelFails() {

                when(chatModel.call(any(Prompt.class)))
                                .thenThrow(
                                                new RuntimeException("AI service unavailable"));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> chatService.processChatMessage(
                                                request("Show plumbers")));

                assertEquals(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                exception.getStatusCode());

                verify(bookingTools, never())
                                .confirmBooking(anyString(), anyString());
        }
}