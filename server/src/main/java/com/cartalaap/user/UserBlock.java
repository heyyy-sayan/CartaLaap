package com.cartalaap.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_blocks")
public class UserBlock {
    @EmbeddedId
    private UserBlockId id;

    @MapsId("blockerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id")
    private User blocker;

    @MapsId("blockedId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id")
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserBlock() {
    }

    public UserBlock(User blocker, User blocked) {
        this.id = new UserBlockId(blocker.getId(), blocked.getId());
        this.blocker = blocker;
        this.blocked = blocked;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public User getBlocked() { return blocked; }
}
