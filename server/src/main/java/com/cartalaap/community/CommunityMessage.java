package com.cartalaap.community;

import java.time.Instant;
import com.cartalaap.user.User;
import jakarta.persistence.*;

@Entity @Table(name="community_messages")
public class CommunityMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="community_id") private Community community;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sender_id") private User sender;
    @Column(length=2000) private String body;
    @Column(name="image_url",length=2048) private String imageUrl;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reply_to_id") private CommunityMessage replyTo;
    @OneToOne(mappedBy="message",fetch=FetchType.LAZY,cascade=CascadeType.ALL,orphanRemoval=true) private CommunityPoll poll;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    protected CommunityMessage() {}
    public CommunityMessage(Community community,User sender,String body,String imageUrl,CommunityMessage replyTo){this.community=community;this.sender=sender;this.body=body;this.imageUrl=imageUrl;this.replyTo=replyTo;}
    @PrePersist void create(){createdAt=Instant.now();}
    public void attachPoll(CommunityPoll poll){this.poll=poll;}
    public Long getId(){return id;} public Community getCommunity(){return community;} public User getSender(){return sender;} public String getBody(){return body;} public String getImageUrl(){return imageUrl;} public CommunityMessage getReplyTo(){return replyTo;} public CommunityPoll getPoll(){return poll;} public Instant getCreatedAt(){return createdAt;}
}
