package com.localconnect.backend.controller;

import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.ChatResponse;
import com.localconnect.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse processChat(@RequestBody ChatRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (request.getUserEmail() == null || request.getUserEmail().isBlank()) {
                request.setUserEmail(auth.getName());
            }
        }
        return chatService.processChatMessage(request);
    }
}
