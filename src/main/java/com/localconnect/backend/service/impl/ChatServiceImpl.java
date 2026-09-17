package com.localconnect.backend.service.impl;

import com.localconnect.backend.config.BookingTools;

import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ChatResponse;

import com.localconnect.backend.entity.ChatMessage;
import com.localconnect.backend.repository.ChatMessageRepository;

import com.localconnect.backend.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

        private final ChatMessageRepository chatMessageRepository;
        private final BookingTools bookingTools;
        private final ChatModel chatModel;

        private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

        // ==========================================
        // SYSTEM PROMPT
        // ==========================================

        private static final String SYSTEM_PROMPT = """
                        You are the LocalConnect AI Assistant.

                        LocalConnect is a local home-services marketplace.

                        Answer questions naturally in English or Hindi,
                        depending on the user's language.

                        You can use tools to:
                        - Search real local services.
                        - Check the user's bookings.
                        - Check booking status.
                        - Prepare a booking proposal.

                        IMPORTANT:

                        1. When someone needs a service, use the
                           recommendServices tool.

                        2. Recommend only actual services returned by
                           the database. Never invent IDs or prices.

                        3. Remember the conversation. If the user says
                           "book the second one", use the ID of the
                           second service from the previous results.

                        4. Before preparing a booking, collect:
                           - Real service ID
                           - Date
                           - Exact time

                        5. If information is missing, ask a question.
                           Never guess an ID, date or time.

                        6. Once everything is available, call
                           proposeBooking.

                        7. proposeBooking does NOT create a real booking.
                           Tell the user to confirm.

                        8. Never claim a booking has been created unless
                           the backend returns a successful booking result.

                        9. For cancellation or rescheduling of an existing
                           booking, direct the user to My Bookings.
                           Those operations are not AI tools in this version.

                        10. Never invent database results.

                        Be helpful, conversational and concise.
                        """;

        // ==========================================
        // MAIN CHAT METHOD
        // ==========================================

        @Override
        public ChatResponse processChatMessage(
                        ChatRequest request) {

                if (request == null
                                || request.getMessage() == null
                                || request.getMessage().isBlank()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Message is required.");
                }

                // ChatController must set this from JWT.
                String userEmail = request.getUserEmail();

                if (userEmail == null || userEmail.isBlank()) {

                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Please log in.");
                }

                userEmail = userEmail.trim();

                String message = request.getMessage().trim();

                if (message.length() > 2000) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Message is too long.");
                }

                String conversationId = request.getConversationId() == null
                                || request.getConversationId().isBlank()
                                                ? UUID.randomUUID().toString()
                                                : request.getConversationId().trim();

                if (conversationId.length() > 128) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid conversation ID.");
                }

                // ======================================
                // 1. CHECK CONVERSATION OWNERSHIP
                // ======================================

                List<ChatMessage> history = chatMessageRepository
                                .findByConversationIdOrderByCreatedAtAsc(
                                                conversationId);

                if (history == null) {
                        history = List.of();
                }

                final String authenticatedEmail = userEmail;

                boolean unauthorized = history.stream()
                                .anyMatch(
                                                chat -> chat.getUserEmail() == null
                                                                || !authenticatedEmail.equalsIgnoreCase(
                                                                                chat.getUserEmail()));

                if (unauthorized) {

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Conversation access denied.");
                }

                // ======================================
                // 2. SAVE USER MESSAGE
                // ======================================

                saveMessage(
                                conversationId,
                                userEmail,
                                "USER",
                                message);

                String lower = message.toLowerCase(Locale.ROOT);

                // ======================================
                // 3. USER CONFIRMS A BOOKING
                // ======================================

                if (lower.equals("confirm booking")
                                || lower.equals("yes, confirm booking")) {

                        try {

                                BookingResponse result = bookingTools.confirmBooking(
                                                userEmail,
                                                conversationId);

                                String reply = "✅ Booking created successfully!"
                                                + "\nBooking ID: #" + result.getId()
                                                + "\nService: "
                                                + result.getServiceTitle()
                                                + "\nDate and time: "
                                                + result.getBookingDate()
                                                + "\nStatus: "
                                                + result.getStatus();

                                return finish(
                                                conversationId,
                                                userEmail,
                                                reply,
                                                "BOOKING_CREATED");

                        } catch (ResponseStatusException exception) {

                                return finish(
                                                conversationId,
                                                userEmail,
                                                "⚠️ " + exception.getReason(),
                                                "CHAT");
                        }
                }

                // ======================================
                // 4. CANCEL AN UNCONFIRMED REQUEST
                // ======================================

                if (lower.equals("cancel request")
                                || lower.equals("cancel booking request")) {

                        bookingTools.cancelRequest(
                                        userEmail,
                                        conversationId);

                        return finish(
                                        conversationId,
                                        userEmail,
                                        "Booking request discarded."
                                                        + " No new booking was created.",
                                        "CHAT");
                }

                // ======================================
                // 5. ASK THE REAL AI MODEL
                // ======================================

                AtomicBoolean proposalStaged = new AtomicBoolean(false);

                Map<String, Object> toolContext = Map.of(
                                "userEmail", userEmail,
                                "conversationId", conversationId,
                                "proposalStaged", proposalStaged);

                String prompt = buildConversation(history, message);

                String reply;

                try {

                        reply = ChatClient.create(chatModel)
                                        .prompt()
                                        .system(
                                                        SYSTEM_PROMPT
                                                                        + "\nCurrent India date and time: "
                                                                        + LocalDateTime.now(INDIA))
                                        .user(prompt)
                                        .tools(bookingTools)
                                        .toolContext(toolContext)
                                        .call()
                                        .content();

                } catch (Exception exception) {

                        log.error(
                                        "AI model failed: {}",
                                        exception.getMessage());

                        // No hardcoded fallback answer.
                        throw new ResponseStatusException(
                                        HttpStatus.SERVICE_UNAVAILABLE,
                                        "AI model is unavailable. Check the "
                                                        + "API token, model and tool support.");
                }

                // ======================================
                // 6. IF AI PREPARED A BOOKING
                // ======================================

                if (proposalStaged.get()) {

                        String summary = bookingTools.getPendingSummary(
                                        userEmail,
                                        conversationId);

                        if (summary == null) {

                                throw new ResponseStatusException(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Booking proposal could not be loaded.");
                        }

                        return finish(
                                        conversationId,
                                        userEmail,
                                        summary,
                                        "BOOKING_PENDING_CONFIRMATION");
                }

                // ======================================
                // 7. RETURN NORMAL AI RESPONSE
                // ======================================

                if (reply == null || reply.isBlank()) {

                        throw new ResponseStatusException(
                                        HttpStatus.SERVICE_UNAVAILABLE,
                                        "AI model returned an empty response.");
                }

                return finish(
                                conversationId,
                                userEmail,
                                reply,
                                "CHAT");
        }

        // ==========================================
        // BUILD CONVERSATION MEMORY
        // ==========================================

        private String buildConversation(
                        List<ChatMessage> history,
                        String currentMessage) {

                StringBuilder conversation = new StringBuilder();

                conversation.append(
                                "Recent conversation:\n");

                int start = Math.max(
                                0,
                                history.size() - 12);

                for (int i = start; i < history.size(); i++) {

                        ChatMessage chat = history.get(i);

                        String content = chat.getContent() == null
                                        ? ""
                                        : chat.getContent();

                        conversation.append(
                                        chat.getSender());

                        conversation.append(": ");

                        conversation.append(
                                        content.substring(
                                                        0,
                                                        Math.min(900, content.length())));

                        conversation.append("\n");
                }

                conversation.append(
                                "\nCurrent user message: ");

                conversation.append(
                                currentMessage);

                return conversation.toString();
        }

        // ==========================================
        // SAVE MESSAGE
        // ==========================================

        private void saveMessage(
                        String conversationId,
                        String userEmail,
                        String sender,
                        String content) {

                ChatMessage chat = ChatMessage.builder()
                                .conversationId(conversationId)
                                .userEmail(userEmail)
                                .sender(sender)
                                .content(content)
                                .createdAt(LocalDateTime.now(INDIA))
                                .build();

                chatMessageRepository.save(chat);
        }

        // ==========================================
        // SAVE AND RETURN RESPONSE
        // ==========================================

        private ChatResponse finish(
                        String conversationId,
                        String userEmail,
                        String reply,
                        String action) {

                saveMessage(
                                conversationId,
                                userEmail,
                                "ASSISTANT",
                                reply);

                return ChatResponse.builder()
                                .reply(reply)
                                .conversationId(conversationId)
                                .actionPerformed(action)
                                .data(null)
                                .build();
        }
}