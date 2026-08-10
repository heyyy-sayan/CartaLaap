package com.cartalaap.community;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPollVoteRepository extends JpaRepository<CommunityPollVote, CommunityPollVoteId> {
    Optional<CommunityPollVote> findByPoll_IdAndUser_Id(Long pollId, Long userId);
}
