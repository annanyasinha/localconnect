package com.localconnect.backend.service.impl;

import com.localconnect.backend.dto.request.BookingCreateRequest;
import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.BookingResponse;
import com.localconnect.backend.dto.response.ChatResponse;
import com.localconnect.backend.dto.response.ServiceListingResponse;
import com.localconnect.backend.entity.ChatMessage;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.RoleName;
import com.localconnect.backend.repository.ChatMessageRepository;
import com.localconnect.backend.repository.UserRepository;
import com.localconnect.backend.service.BookingService;
import com.localconnect.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    // Concurrent in-memory state tracking pending bookings per conversation ID
    private final Map<String, Long> pendingBookingState = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private ChatModel chatModel;

    private static final String SYSTEM_PROMPT = """
            You are LocalConnect AI Assistant — an intelligent, natural, conversational AI agent for booking and managing local home services in Jamshedpur.
            You support both English and Hindi. Speak naturally, politely, and clearly like ChatGPT or Claude.

            You have access to real-time tools to inspect the database and perform actions:
            - recommendServicesFunction: Search available services by category (Tutor, Plumber, Electrician, Cook, Househelp, Carpenter, Babysitter) or city.
            - bookServiceFunction: Create a booking in the database for a service ID, date/time, and user email.
            - cancelBookingFunction: Cancel an existing booking by booking ID.
            - rescheduleBookingFunction: Reschedule a booking to a new date.
            - checkBookingStatusFunction: Check status or view active bookings.

            GUIDELINES:
            1. When the user asks for recommendations, search, or mentions a problem/need (e.g. "teach me maths", "fix my washroom", "prepare for wedding", "need a cook"), USE `recommendServicesFunction` to query real services from the database and present them clearly with their IDs, names, prices, and locations.
            2. When the user selects a service to book (e.g. "book 51", "book service 51", "book the first one"), if date/time is provided (e.g. "tomorrow at 5 PM"), CALL `bookServiceFunction`. If date/time is missing, ask the user politely for their preferred date and time.
            3. Present booking confirmations clearly with Service Name, Date & Time, Booking ID, and Status.
            """;

    @Override
    public ChatResponse processChatMessage(ChatRequest request) {
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        String userEmail = (request.getUserEmail() != null && !request.getUserEmail().isBlank())
                ? request.getUserEmail()
                : "guest@localconnect.com";

        String userMessage = request.getMessage() != null ? request.getMessage().trim() : "";
        String lowerMsg = userMessage.toLowerCase();

        // 1. Conversation Isolation Check
        List<ChatMessage> history = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        boolean belongsToSomeoneElse = history.stream()
                .anyMatch(msg -> !userEmail.equalsIgnoreCase(msg.getUserEmail()));

        if (belongsToSomeoneElse) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Conversation access denied");
        }

        // 2. Persist User Message to Chat Memory
        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .userEmail(userEmail)
                .sender("USER")
                .content(userMessage)
                .createdAt(LocalDateTime.now())
                .build());

        // 3. Refresh Multi-Turn Chat History from Memory
        history = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        User user = userRepository.findByEmail(userEmail).orElse(null);
        String userName = (user != null && user.getFullName() != null) ? user.getFullName() : "Guest User";

        String reply;
        String action = "CHAT";
        Object responseData = null;

        // 4. Conversational LLM Execution with Spring AI Tool Calling
        if (chatModel != null) {
            try {
                String historyContext = buildHistoryContext(history);
                String promptText = String.format("""
                        %s

                        User Profile: Name=%s, Email=%s

                        Recent Chat Memory:
                        %s

                        User Request: %s
                        """, SYSTEM_PROMPT, userName, userEmail, historyContext, userMessage);

                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .toolNames(java.util.Set.of("bookServiceFunction", "cancelBookingFunction",
                                "rescheduleBookingFunction", "checkBookingStatusFunction", "recommendServicesFunction"))
                        .build();

                Prompt prompt = new Prompt(promptText, options);
                reply = chatModel.call(prompt).getResult().getOutput().getText();
                log.info("LLM Agent generated reply: {}", reply);
            } catch (Exception ex) {
                log.warn("Spring AI LLM call failed, executing rule fallback agent: {}", ex.getMessage());
                reply = executeRuleFallback(userMessage, userEmail, history, userName, conversationId);
            }
        } else {
            reply = executeRuleFallback(userMessage, userEmail, history, userName, conversationId);
        }

        if (reply != null) {
            if (reply.contains("cancelled successfully")) {
                action = "BOOKING_CANCELLED";
            } else if (reply.contains("Booking Summary") || reply.contains("has been booked")
                    || reply.contains("Scheduled") || reply.contains("Booking Confirmed")
                    || reply.contains("Booking Request Submitted")) {
                action = "BOOKING_CREATED";
            } else if (reply.contains("Top Services") || reply.contains("Found")) {
                action = "RECOMMENDATIONS";
            }
        }

        // 5. Persist Assistant Response to Chat Memory
        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .userEmail(userEmail)
                .sender("ASSISTANT")
                .content(reply)
                .createdAt(LocalDateTime.now())
                .build());

        return ChatResponse.builder()
                .reply(reply)
                .conversationId(conversationId)
                .actionPerformed(action)
                .data(responseData)
                .build();
    }

    private String buildHistoryContext(List<ChatMessage> history) {
        if (history == null || history.isEmpty())
            return "None";
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            sb.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String executeRuleFallback(String userMessage, String userEmail, List<ChatMessage> history, String userName,
            String conversationId) {
        String lowerMsg = userMessage.toLowerCase();

        // 1. Greeting
        if (isGreetingOnly(userMessage)) {
            pendingBookingState.remove(conversationId);
            return String.format(
                    "👋 Hello %s! Welcome to LocalConnect AI Assistant. How can I help you today?\n\n1️⃣ Search services (e.g. 'Show home tutors' or 'Plumbers')\n2️⃣ Book a service (e.g. 'Book service 51')\n3️⃣ Manage bookings ('Show my bookings', 'Cancel booking #5')",
                    userName);
        }

        // 2. Name & Profile Inquiry
        if (lowerMsg.contains("my name") || lowerMsg.contains("who am i") || lowerMsg.contains("my email")
                || lowerMsg.contains("my profile")) {
            pendingBookingState.remove(conversationId);
            return String.format("👤 Your name is %s (Email: %s). How can I assist you today?", userName, userEmail);
        }

        // 3. Assistant Identity Inquiry
        if (lowerMsg.contains("who are you") || lowerMsg.contains("what is your name")
                || lowerMsg.contains("what can you do")) {
            pendingBookingState.remove(conversationId);
            return "🤖 I am LocalConnect AI Assistant. I can help you search local services, book tickets/services, manage existing bookings, or cancel bookings! How can I assist you today?";
        }

        // 4. Show My Bookings
        if (lowerMsg.contains("status") || lowerMsg.contains("my booking") || lowerMsg.contains("स्थिति")) {
            pendingBookingState.remove(conversationId);
            Matcher matcher = Pattern.compile("(\\d+)").matcher(lowerMsg);
            if (matcher.find()) {
                Long bookingId = Long.parseLong(matcher.group(1));
                try {
                    BookingResponse res = bookingService.getBookingStatus(bookingId, userEmail);
                    return String.format(
                            "📋 Booking Summary\n\nService: %s\nDate & Time: %s\nStatus: %s\nBooking ID: #%d",
                            res.getServiceTitle() != null ? res.getServiceTitle() : "Service #" + res.getServiceId(),
                            res.getBookingDate() != null ? res.getBookingDate().toString().replace("T", " at ")
                                    : "Scheduled",
                            res.getStatus(), res.getId());
                } catch (Exception e) {
                    return "⚠️ Booking status error: " + e.getMessage();
                }
            } else {
                List<BookingResponse> myBookings = bookingService.getMyBookings(userEmail);
                if (myBookings.isEmpty()) {
                    return "You currently have no active bookings. Ask me to book a service anytime!";
                } else {
                    StringBuilder sb = new StringBuilder("📋 Here are your current bookings:\n");
                    for (BookingResponse b : myBookings) {
                        sb.append(String.format("• Booking #%d - %s [%s]\n", b.getId(), b.getServiceTitle(),
                                b.getStatus()));
                    }
                    return sb.toString();
                }
            }
        }

        // 5. Cancel Booking
        if (lowerMsg.contains("cancel") || lowerMsg.contains("रद्द")) {
            pendingBookingState.remove(conversationId);
            Matcher matcher = Pattern.compile("(\\d+)").matcher(lowerMsg);
            if (matcher.find()) {
                Long bookingId = Long.parseLong(matcher.group(1));
                try {
                    BookingResponse res = bookingService.cancelBooking(bookingId, userEmail);
                    return String.format("✅ Booking #%d has been cancelled successfully.\nService: %s\nStatus: %s",
                            res.getId(),
                            res.getServiceTitle() != null ? res.getServiceTitle() : "Service #" + bookingId,
                            res.getStatus());
                } catch (Exception e) {
                    return "⚠️ Could not cancel booking: " + e.getMessage();
                }
            } else {
                return "To cancel a booking, please mention the booking ID (e.g., 'Cancel booking #1').";
            }
        }

        // 6. Booking Intent
        if (isBookingIntent(lowerMsg)) {
            Long serviceId = extractServiceIdFromMsgOrHistory(lowerMsg, history);

            if (serviceId != null) {
                if (isValidDateTimeExpression(userMessage)) {
                    LocalDateTime parsedDate = parseBookingDate(userMessage);
                    if (parsedDate == null) {
                        return "I couldn't parse a valid future date and time from your request. Please specify a future date and time (e.g., 'Tomorrow at 5 PM').";
                    }
                    BookingCreateRequest bookingRequest = new BookingCreateRequest();
                    bookingRequest.setServiceId(serviceId);
                    bookingRequest.setMessage("Booked via LocalConnect AI Chatbot: " + userMessage);
                    bookingRequest.setBookingDate(parsedDate);

                    try {
                        BookingResponse res = bookingService.createBooking(bookingRequest, userEmail);
                        pendingBookingState.remove(conversationId);
                        return formatBookingSummary(res, extractDateStrFromUserMsg(userMessage));
                    } catch (Exception e) {
                        pendingBookingState.remove(conversationId);
                        return "⚠️ Unable to complete booking: " + e.getMessage() + ". Please verify service ID.";
                    }
                } else {
                    pendingBookingState.put(conversationId, serviceId);
                    return String.format(
                            "🗓️ Great choice! You selected Service #%d.\nWhat date and time would you like to schedule this booking for? (e.g., Tomorrow at 5 PM)",
                            serviceId);
                }
            } else {
                return "Which service would you like to book? Please state the service ID (e.g., 'Book service 51') or search services first.";
            }
        }

        // 7. Waiting for Date
        if (pendingBookingState.containsKey(conversationId) || isReplyingToPendingBookingDate(history, lowerMsg)) {
            Long pendingServiceId = pendingBookingState.getOrDefault(conversationId, extractPendingServiceId(history));
            if (pendingServiceId != null) {
                if (isValidDateTimeExpression(userMessage)) {
                    LocalDateTime parsedDate = parseBookingDate(userMessage);
                    if (parsedDate == null) {
                        return "I couldn't parse a valid future date and time. Please specify a future date and time (e.g., 'Tomorrow at 5 PM').";
                    }
                    BookingCreateRequest bookingRequest = new BookingCreateRequest();
                    bookingRequest.setServiceId(pendingServiceId);
                    bookingRequest.setMessage("Booked via LocalConnect AI Chatbot: " + userMessage);
                    bookingRequest.setBookingDate(parsedDate);

                    try {
                        BookingResponse res = bookingService.createBooking(bookingRequest, userEmail);
                        pendingBookingState.remove(conversationId);
                        return formatBookingSummary(res, userMessage);
                    } catch (Exception e) {
                        pendingBookingState.remove(conversationId);
                        return "⚠️ Unable to complete booking: " + e.getMessage() + ". Please verify service ID.";
                    }
                } else {
                    return "I couldn't understand the date and time. Try 'Tomorrow at 5 PM' or type 'cancel'.";
                }
            }
        }

        // 8. Service Search & Recommendation
        if (lowerMsg.contains("wedding") || lowerMsg.contains("marriage") || lowerMsg.contains("party")
                || lowerMsg.contains("event")) {
            List<ServiceListingResponse> services = bookingService.recommendServices("", "");
            if (!services.isEmpty()) {
                StringBuilder sb = new StringBuilder(
                        "🎉 Congratulations! To help you prepare for your event, here are top available local services (Cooks, Househelp, Tutors, Plumbers) you can book right now:\n");
                for (int i = 0; i < Math.min(5, services.size()); i++) {
                    ServiceListingResponse s = services.get(i);
                    sb.append(String.format("• ID #%d: %s (%s) - ₹%.2f [%s]\n", s.getId(), s.getTitle(),
                            s.getCategory(), s.getPrice(), s.getCity()));
                }
                sb.append("\n👉 Reply 'Book service <ID>' to select a service!");
                return sb.toString();
            }
        }

        String categoryKeyword = extractCategoryFromKeywords(lowerMsg);
        if (!categoryKeyword.isEmpty()) {
            List<ServiceListingResponse> services = bookingService.recommendServices(categoryKeyword, "");
            if (!services.isEmpty()) {
                StringBuilder sb = new StringBuilder(
                        "🔍 Here are available " + categoryKeyword + " services you can book:\n");
                for (int i = 0; i < Math.min(5, services.size()); i++) {
                    ServiceListingResponse s = services.get(i);
                    sb.append(String.format("• ID #%d: %s (%s) - ₹%.2f [%s]\n", s.getId(), s.getTitle(),
                            s.getCategory(), s.getPrice(), s.getCity()));
                }
                sb.append("\n👉 Reply 'Book service <ID>' (or 'Book the first one') to select a service!");
                return sb.toString();
            }
        } else if (isServiceSearchQuery(lowerMsg)) {
            List<ServiceListingResponse> services = bookingService.recommendServices("", "");
            if (!services.isEmpty()) {
                StringBuilder sb = new StringBuilder(
                        "👋 Welcome to LocalConnect AI Assistant.\nHere are top available services you can book right now:\n");
                for (int i = 0; i < Math.min(5, services.size()); i++) {
                    ServiceListingResponse s = services.get(i);
                    sb.append(String.format("• ID #%d: %s (%s) - ₹%.2f [%s]\n", s.getId(), s.getTitle(),
                            s.getCategory(), s.getPrice(), s.getCity()));
                }
                sb.append("\n👉 Reply 'Book service <ID>' (or 'Book the first one') to select a service!");
                return sb.toString();
            }
        }

        return "👋 Hello! I am your LocalConnect AI Assistant. I can help you:\n1️⃣ Search local services\n2️⃣ Book services & tickets\n3️⃣ Manage bookings ('Show my bookings', 'Cancel booking #5')\n\nHow can I help you today?";
    }

    private LocalDateTime parseBookingDate(String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            return null;
        String lower = userMessage.toLowerCase().trim();
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        ZonedDateTime targetDate = null;
        if (lower.contains("tomorrow")) {
            targetDate = now.plusDays(1);
        } else if (lower.contains("today") || lower.contains("tonight")) {
            targetDate = now;
        } else if (lower.contains("next week")) {
            targetDate = now.plusWeeks(1);
        }

        int hour = 11;
        int minute = 0;
        Matcher timeMatcher = Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", Pattern.CASE_INSENSITIVE)
                .matcher(lower);
        while (timeMatcher.find()) {
            try {
                int h = Integer.parseInt(timeMatcher.group(1));
                int m = timeMatcher.group(2) != null ? Integer.parseInt(timeMatcher.group(2)) : 0;
                String ampm = timeMatcher.group(3);
                if (ampm != null) {
                    if (ampm.equalsIgnoreCase("pm") && h < 12)
                        h += 12;
                    else if (ampm.equalsIgnoreCase("am") && h == 12)
                        h = 0;
                }
                if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                    hour = h;
                    minute = m;
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        if (targetDate != null) {
            LocalDateTime ldt = targetDate.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                    .toLocalDateTime();
            if (ldt.isAfter(now.toLocalDateTime())) {
                return ldt;
            }
        }

        // Try ISO date pattern yyyy-MM-dd
        Matcher isoMatcher = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})(?:[T\\s]+(\\d{2}):(\\d{2}))?\\b")
                .matcher(userMessage);
        if (isoMatcher.find()) {
            try {
                int y = Integer.parseInt(isoMatcher.group(1));
                int m = Integer.parseInt(isoMatcher.group(2));
                int d = Integer.parseInt(isoMatcher.group(3));
                int h = isoMatcher.group(4) != null ? Integer.parseInt(isoMatcher.group(4)) : 11;
                int min = isoMatcher.group(5) != null ? Integer.parseInt(isoMatcher.group(5)) : 0;
                LocalDateTime ldt = LocalDateTime.of(y, m, d, h, min);
                if (ldt.isAfter(now.toLocalDateTime())) {
                    return ldt;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean isValidDateTimeExpression(String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            return false;
        String lower = userMessage.toLowerCase().trim();

        if (lower.matches("^\\s*(#|id|service|option)?\\s*\\d+\\s*$") ||
                lower.contains("tutor") || lower.contains("plumb") || lower.contains("electric") ||
                lower.contains("cook") || lower.contains("househelp") || lower.contains("clean") ||
                lower.contains("carpent") || lower.contains("first") || lower.contains("second") ||
                lower.contains("third") || lower.contains("fourth") || lower.contains("fifth")) {
            return false;
        }

        boolean hasRelativeDay = lower.contains("today") || lower.contains("tomorrow") || lower.contains("tonight")
                || lower.contains("next week");
        boolean hasDayOfWeek = lower.contains("monday") || lower.contains("tuesday") || lower.contains("wednesday") ||
                lower.contains("thursday") || lower.contains("friday") || lower.contains("saturday")
                || lower.contains("sunday");
        boolean hasMonth = lower.matches(
                ".*\\b(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)\\b.*");
        boolean hasTimeSpecifier = lower
                .matches(".*\\b(\\d{1,2}:\\d{2}|\\d{1,2}\\s*(am|pm)|o'clock|oclock|at\\s+\\d{1,2})\\b.*");
        boolean hasIsoDate = lower.matches(".*\\b\\d{4}-\\d{2}-\\d{2}\\b.*")
                || lower.matches(".*\\b\\d{1,2}[/-]\\d{1,2}\\b.*");

        return hasRelativeDay || hasDayOfWeek || hasMonth || hasTimeSpecifier || hasIsoDate;
    }

    private boolean isServiceSearchQuery(String lowerMsg) {
        return lowerMsg.contains("recommend") || lowerMsg.contains("top services") || lowerMsg.contains("problem")
                || lowerMsg.contains("need someone") || lowerMsg.contains("fix") ||
                lowerMsg.contains("wedding") || lowerMsg.contains("marriage") || lowerMsg.contains("party")
                || lowerMsg.contains("event") || lowerMsg.contains("celebrate") || lowerMsg.contains("prep") ||
                lowerMsg.contains("tutor") || lowerMsg.contains("teacher") || lowerMsg.contains("math")
                || lowerMsg.contains("teach") || lowerMsg.contains("tuition") || lowerMsg.contains("study")
                || lowerMsg.contains("class") || lowerMsg.contains("school") ||
                lowerMsg.contains("plumb") || lowerMsg.contains("pipe") || lowerMsg.contains("tap")
                || lowerMsg.contains("leak") || lowerMsg.contains("washroom") || lowerMsg.contains("bathroom")
                || lowerMsg.contains("toilet") || lowerMsg.contains("flush") || lowerMsg.contains("drain")
                || lowerMsg.contains("sink") || lowerMsg.contains("water") ||
                lowerMsg.contains("electric") || lowerMsg.contains("wire") || lowerMsg.contains("light")
                || lowerMsg.contains("switch") || lowerMsg.contains("fan") || lowerMsg.contains("fuse")
                || lowerMsg.contains("power") ||
                lowerMsg.contains("cook") || lowerMsg.contains("chef") || lowerMsg.contains("meal")
                || lowerMsg.contains("food") || lowerMsg.contains("kitchen") || lowerMsg.contains("dinner")
                || lowerMsg.contains("lunch") ||
                lowerMsg.contains("househelp") || lowerMsg.contains("clean") || lowerMsg.contains("maid")
                || lowerMsg.contains("dish") || lowerMsg.contains("mop") || lowerMsg.contains("dust")
                || lowerMsg.contains("sweep") ||
                lowerMsg.contains("carpent") || lowerMsg.contains("wood") || lowerMsg.contains("furniture")
                || lowerMsg.contains("door") || lowerMsg.contains("table") || lowerMsg.contains("bed")
                || lowerMsg.contains("cabinet") ||
                lowerMsg.contains("baby") || lowerMsg.contains("nanny") || lowerMsg.contains("child")
                || lowerMsg.contains("kid") || lowerMsg.contains("sitter");
    }

    private String formatBookingSummary(BookingResponse res, String dateTimeUserStr) {
        String serviceName = res.getServiceTitle() != null ? res.getServiceTitle() : "Service #" + res.getServiceId();
        String displayDate = (res.getBookingDate() != null)
                ? res.getBookingDate().toString().replace("T", " at ")
                : (dateTimeUserStr != null && !dateTimeUserStr.isBlank() ? capitalizeWords(dateTimeUserStr)
                        : "Scheduled");

        String statusHeader = "PENDING".equalsIgnoreCase(res.getStatus()) ? "Booking Request Submitted"
                : "Booking Confirmed";

        return String.format("""
                🎉 %s!

                Service: %s
                Date & Time: %s
                Status: %s
                Booking ID: #%d
                """, statusHeader, serviceName, displayDate, res.getStatus(), res.getId()).trim();
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank())
            return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String extractDateStrFromUserMsg(String userMsg) {
        if (userMsg == null)
            return "Tomorrow at 11:00 AM";
        String lower = userMsg.toLowerCase();
        if (lower.contains("at ")) {
            int idx = lower.indexOf("at ");
            return userMsg.substring(Math.max(0, idx - 10)).trim();
        }
        return userMsg;
    }

    private boolean isGreetingOnly(String text) {
        String lower = text.toLowerCase().replaceAll("[^a-z]", " ").trim();
        return lower.equals("hi") || lower.equals("hello") || lower.equals("hey") ||
                lower.equals("namaste") || lower.equals("good morning") || lower.equals("good evening") ||
                lower.equals("hi there") || lower.equals("hello there");
    }

    private boolean isBookingIntent(String lowerMsg) {
        if (lowerMsg.contains("cancel") || lowerMsg.contains("status"))
            return false;
        if (lowerMsg.contains("book") || lowerMsg.contains("बुक") ||
                lowerMsg.contains("first one") || lowerMsg.contains("1st") ||
                lowerMsg.contains("second one") || lowerMsg.contains("2nd") ||
                lowerMsg.contains("third one") || lowerMsg.contains("3rd")) {
            return true;
        }
        return lowerMsg.matches("^\\s*(#|id|service|option)?\\s*\\d+\\s*$");
    }

    private boolean isReplyingToPendingBookingDate(List<ChatMessage> history, String lowerMsg) {
        if (history == null || history.size() < 2)
            return false;
        ChatMessage lastAssistantMsg = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("ASSISTANT".equalsIgnoreCase(history.get(i).getSender())) {
                lastAssistantMsg = history.get(i);
                break;
            }
        }
        return lastAssistantMsg != null
                && lastAssistantMsg.getContent().contains("What date and time would you like to schedule");
    }

    private Long extractPendingServiceId(List<ChatMessage> history) {
        if (history == null)
            return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if ("ASSISTANT".equalsIgnoreCase(msg.getSender()) && msg.getContent().contains("selected Service #")) {
                Matcher matcher = Pattern.compile("selected Service #(\\d+)").matcher(msg.getContent());
                if (matcher.find()) {
                    return Long.parseLong(matcher.group(1));
                }
            }
        }
        return null;
    }

    private Long extractServiceIdFromMsgOrHistory(String lowerMsg, List<ChatMessage> history) {
        List<Long> recentServiceIds = extractRecentServiceIdsFromHistory(history);

        Matcher matcher = Pattern.compile("(\\d+)").matcher(lowerMsg);
        if (matcher.find()) {
            long num = Long.parseLong(matcher.group(1));

            if (recentServiceIds.contains(num)) {
                return num;
            }

            if (!recentServiceIds.isEmpty() && num >= 1 && num <= recentServiceIds.size()) {
                return recentServiceIds.get((int) num - 1);
            }

            return num;
        }

        if (!recentServiceIds.isEmpty()) {
            if (lowerMsg.contains("first") || lowerMsg.contains("1st")) {
                return recentServiceIds.get(0);
            } else if ((lowerMsg.contains("second") || lowerMsg.contains("2nd")) && recentServiceIds.size() > 1) {
                return recentServiceIds.get(1);
            } else if ((lowerMsg.contains("third") || lowerMsg.contains("3rd")) && recentServiceIds.size() > 2) {
                return recentServiceIds.get(2);
            } else if ((lowerMsg.contains("fourth") || lowerMsg.contains("4th")) && recentServiceIds.size() > 3) {
                return recentServiceIds.get(3);
            } else if ((lowerMsg.contains("fifth") || lowerMsg.contains("5th")) && recentServiceIds.size() > 4) {
                return recentServiceIds.get(4);
            }
            return recentServiceIds.get(0);
        }
        return null;
    }

    private List<Long> extractRecentServiceIdsFromHistory(List<ChatMessage> history) {
        List<Long> ids = new ArrayList<>();
        if (history == null)
            return ids;

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if ("ASSISTANT".equalsIgnoreCase(msg.getSender())) {
                Matcher matcher = Pattern.compile("• ID #(\\d+)").matcher(msg.getContent());
                while (matcher.find()) {
                    ids.add(Long.parseLong(matcher.group(1)));
                }
                if (!ids.isEmpty())
                    break;
            }
        }
        return ids;
    }

    private String extractCategoryFromKeywords(String lowerMsg) {
        if (lowerMsg.contains("tutor") || lowerMsg.contains("teacher") || lowerMsg.contains("math")
                || lowerMsg.contains("teach") || lowerMsg.contains("tuition") || lowerMsg.contains("study")
                || lowerMsg.contains("class") || lowerMsg.contains("school"))
            return "Tutor";
        if (lowerMsg.contains("plumb") || lowerMsg.contains("pipe") || lowerMsg.contains("tap")
                || lowerMsg.contains("leak") || lowerMsg.contains("washroom") || lowerMsg.contains("bathroom")
                || lowerMsg.contains("toilet") || lowerMsg.contains("flush") || lowerMsg.contains("drain")
                || lowerMsg.contains("sink") || lowerMsg.contains("water"))
            return "Plumber";
        if (lowerMsg.contains("electric") || lowerMsg.contains("wire") || lowerMsg.contains("light")
                || lowerMsg.contains("switch") || lowerMsg.contains("fan") || lowerMsg.contains("fuse")
                || lowerMsg.contains("power"))
            return "Electrician";
        if (lowerMsg.contains("cook") || lowerMsg.contains("chef") || lowerMsg.contains("meal")
                || lowerMsg.contains("food") || lowerMsg.contains("kitchen") || lowerMsg.contains("dinner")
                || lowerMsg.contains("lunch") || lowerMsg.contains("cooking"))
            return "Cook";
        if (lowerMsg.contains("househelp") || lowerMsg.contains("clean") || lowerMsg.contains("maid")
                || lowerMsg.contains("dish") || lowerMsg.contains("mop") || lowerMsg.contains("dust")
                || lowerMsg.contains("sweep"))
            return "Househelp";
        if (lowerMsg.contains("carpent") || lowerMsg.contains("wood") || lowerMsg.contains("furniture")
                || lowerMsg.contains("door") || lowerMsg.contains("table") || lowerMsg.contains("bed")
                || lowerMsg.contains("cabinet"))
            return "Carpenter";
        if (lowerMsg.contains("baby") || lowerMsg.contains("nanny") || lowerMsg.contains("child")
                || lowerMsg.contains("kid") || lowerMsg.contains("sitter"))
            return "Babysitter";
        return "";
    }
}
