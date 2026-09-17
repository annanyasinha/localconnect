package com.localconnect.backend.controller;

import com.localconnect.backend.config.ChatRateLimiter;

import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.ChatResponse;

import com.localconnect.backend.service.ChatService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final ChatRateLimiter chatRateLimiter;

    @PostMapping
    public ChatResponse processChat(
            @RequestBody ChatRequest request,
            Principal principal) {

        if (principal == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in to use the chatbot");
        }

        String userEmail = principal.getName();

        if (!chatRateLimiter.allowRequest(userEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Chat limit exceeded. Please try again shortly.");
        }

        request.setUserEmail(userEmail);

        return chatService.processChatMessage(request);
    }
}