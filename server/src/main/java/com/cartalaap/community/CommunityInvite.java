package com.cartalaap.community;

import java.time.Instant;
import com.cartalaap.user.User;
import jakarta.persistence.*;

@Entity @Table(name="community_invites")
public class CommunityInvite {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="community_id") private Community community;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="inviter_id") private User inviter;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invitee_id") private User invitee;
    @Enumerated(EnumType.STRING) @Column(name="invite_status",nullable=false,length=20) private InviteStatus status;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected CommunityInvite() {}
    public CommunityInvite(Community community,User inviter,User invitee){this.community=community;this.inviter=inviter;this.invitee=invitee;this.status=InviteStatus.PENDING;}
    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=Instant.now();}
    public void renew(User nextInviter){inviter=nextInviter;status=InviteStatus.PENDING;updatedAt=Instant.now();}
    public void accept(){status=InviteStatus.ACCEPTED;} public void decline(){status=InviteStatus.DECLINED;}
    public Long getId(){return id;} public Community getCommunity(){return community;} public User getInviter(){return inviter;} public User getInvitee(){return invitee;} public InviteStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
}
