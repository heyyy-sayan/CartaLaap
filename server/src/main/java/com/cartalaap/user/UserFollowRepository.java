package com.cartalaap.user;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFollowRepository extends JpaRepository<UserFollow, FollowId> {
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
    long countByFollowing_Id(Long followingId);
    long countByFollower_Id(Long followerId);
    void deleteByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    @EntityGraph(attributePaths = "follower")
    List<UserFollow> findByFollowing_IdOrderByCreatedAtDesc(Long followingId, Pageable pageable);

    @EntityGraph(attributePaths = "following")
    List<UserFollow> findByFollower_IdOrderByCreatedAtDesc(Long followerId, Pageable pageable);
}
