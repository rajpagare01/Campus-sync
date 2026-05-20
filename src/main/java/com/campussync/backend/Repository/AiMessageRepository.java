package com.campussync.backend.Repository;

import com.campussync.backend.Model.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findTop10ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<AiMessage> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    long countByConversationId(Long conversationId);

    void deleteByConversationId(Long conversationId);
}
