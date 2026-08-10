package com.cartalaap.community;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class CommunityPollVoteId implements Serializable {
    private Long pollId;
    private Long userId;
    protected CommunityPollVoteId() {}
    public CommunityPollVoteId(Long pollId, Long userId) { this.pollId = pollId; this.userId = userId; }
    public Long getPollId() { return pollId; }
    public Long getUserId() { return userId; }
    @Override public boolean equals(Object value) { return value instanceof CommunityPollVoteId other && Objects.equals(pollId, other.pollId) && Objects.equals(userId, other.userId); }
    @Override public int hashCode() { return Objects.hash(pollId, userId); }
}
