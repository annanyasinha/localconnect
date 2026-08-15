package com.localconnect.backend.service;

import com.localconnect.backend.dto.request.ChatRequest;
import com.localconnect.backend.dto.response.ChatResponse;

public interface ChatService {
    ChatResponse processChatMessage(ChatRequest request);
}
