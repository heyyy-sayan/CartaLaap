package com.cartalaap.moment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
@Table(name = "moments")
public class Moment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id")
    private User author;
    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;
    @Column(length = 300)
    private String caption;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    protected Moment() {}
    public Moment(User author, String imageUrl, String caption) { this.author = author; this.imageUrl = imageUrl; this.caption = caption; }
    @PrePersist void onCreate() { createdAt = Instant.now(); expiresAt = createdAt.plus(24, ChronoUnit.HOURS); }
    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getImageUrl() { return imageUrl; }
    public String getCaption() { return caption; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
