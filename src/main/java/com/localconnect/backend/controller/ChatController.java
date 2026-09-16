package com.localconnect.backend.controller;

import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.ChatResponse;
import com.localconnect.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse processChat(
            @RequestBody ChatRequest request,
            Principal principal
    ) {
        request.setUserEmail(principal.getName());

        return chatService.processChatMessage(request);
    }
}
