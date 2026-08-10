package com.cartalaap.topic;

import java.time.Instant;

import com.cartalaap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "community_topics")
public class CommunityTopic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String slug;
    @Column(nullable = false, unique = true, length = 80)
    private String name;
    @Column(length = 240)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id")
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CommunityTopic() {}

    public CommunityTopic(String slug, String name, String description, User createdBy) {
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
