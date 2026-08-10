package com.cartalaap.community;

import java.time.Instant;
import com.cartalaap.user.User;
import jakarta.persistence.*;

@Entity @Table(name="communities")
public class Community {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=50) private String slug;
    @Column(length=300) private String description;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="creator_id") private User creator;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    protected Community() {}
    public Community(String slug,String description,User creator){this.slug=slug;this.description=description;this.creator=creator;}
    @PrePersist void create(){createdAt=Instant.now();}
    public Long getId(){return id;} public String getSlug(){return slug;} public String getDescription(){return description;}
    public User getCreator(){return creator;} public Instant getCreatedAt(){return createdAt;}
}
