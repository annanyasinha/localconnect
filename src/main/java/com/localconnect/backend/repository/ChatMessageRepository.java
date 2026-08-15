package com.localconnect.backend.repository;

import com.localconnect.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<ChatMessage> findByUserEmailOrderByCreatedAtAsc(String userEmail);
}
