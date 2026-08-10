package com.cartalaap.article;

import java.time.Instant;

import com.cartalaap.user.User;
import com.cartalaap.topic.CommunityTopic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles")
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id")
    private User author;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String body;
    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "topic_slug", referencedColumnName = "slug")
    private CommunityTopic topic;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    protected Article() {}
    public Article(User author, String title, String body, String coverImageUrl, CommunityTopic topic) {
        this.author = author; this.title = title; this.body = body; this.coverImageUrl = coverImageUrl; this.topic = topic;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public void update(String title, String body, String coverImageUrl, CommunityTopic topic) { this.title = title; this.body = body; this.coverImageUrl = coverImageUrl; this.topic = topic; }
    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public CommunityTopic getTopic() { return topic; }
    public String getTopicSlug() { return topic == null ? null : topic.getSlug(); }
    public String getTopicName() { return topic == null ? null : topic.getName(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
