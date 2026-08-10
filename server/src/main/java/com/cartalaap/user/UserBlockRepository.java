package com.cartalaap.user;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {
    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
    void deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    @EntityGraph(attributePaths = "blocked")
    List<UserBlock> findByBlocker_IdOrderByCreatedAtDesc(Long blockerId);
}
