package com.cartalaap.message;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<DirectMessage> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);
    Optional<DirectMessage> findFirstByConversation_IdOrderByCreatedAtDesc(Long conversationId);
    long countByConversation_IdAndSender_IdNotAndReadAtIsNull(Long conversationId, Long userId);
}
