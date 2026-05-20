package com.campussync.backend.Repository;

import com.campussync.backend.Model.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);

    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
