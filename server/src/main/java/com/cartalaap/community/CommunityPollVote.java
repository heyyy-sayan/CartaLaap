package com.cartalaap.community;

import java.time.Instant;

import com.cartalaap.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "community_poll_votes")
public class CommunityPollVote {
    @EmbeddedId private CommunityPollVoteId id;
    @MapsId("pollId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poll_id") private CommunityPoll poll;
    @MapsId("userId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "option_id") private CommunityPollOption option;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CommunityPollVote() {}
    public CommunityPollVote(CommunityPoll poll, User user, CommunityPollOption option) { this.id = new CommunityPollVoteId(poll.getId(), user.getId()); this.poll = poll; this.user = user; this.option = option; }
    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public void choose(CommunityPollOption option) { this.option = option; }
    public CommunityPollVoteId getId() { return id; }
    public CommunityPollOption getOption() { return option; }
}
