package com.cartalaap.vote;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostVoteRepository extends JpaRepository<PostVote, PostVoteId> {
    Optional<PostVote> findByPost_IdAndUser_Id(Long postId, Long userId);
    long countByPost_IdAndValue(Long postId, short value);
}
