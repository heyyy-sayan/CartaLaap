package com.cartalaap.message;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByUserOne_IdAndUserTwo_Id(Long userOneId, Long userTwoId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO conversations (user_one_id, user_two_id, created_at, updated_at)
            VALUES (:userOneId, :userTwoId, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    int insertPairIfMissing(@Param("userOneId") Long userOneId, @Param("userTwoId") Long userTwoId);

    @EntityGraph(attributePaths = { "userOne", "userTwo" })
    @Query("SELECT c FROM Conversation c WHERE c.userOne.id = :userId OR c.userTwo.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findInbox(@Param("userId") Long userId);

    @EntityGraph(attributePaths = { "userOne", "userTwo" })
    @Query("SELECT c FROM Conversation c WHERE c.id = :id")
    Optional<Conversation> findDetailedById(@Param("id") Long id);
}
