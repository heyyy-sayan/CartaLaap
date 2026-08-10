package com.cartalaap.community;

import java.time.Instant;
import com.cartalaap.user.User;
import jakarta.persistence.*;

@Entity @Table(name="community_members")
public class CommunityMember {
    @EmbeddedId private CommunityMemberId id;
    @MapsId("communityId") @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="community_id") private Community community;
    @MapsId("userId") @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(name="member_role",nullable=false,length=20) private CommunityRole role;
    @Column(name="joined_at",nullable=false,updatable=false) private Instant joinedAt;
    protected CommunityMember() {}
    public CommunityMember(Community community,User user,CommunityRole role){this.id=new CommunityMemberId(community.getId(),user.getId());this.community=community;this.user=user;this.role=role;}
    @PrePersist void create(){joinedAt=Instant.now();}
    public Community getCommunity(){return community;} public User getUser(){return user;} public CommunityRole getRole(){return role;} public Instant getJoinedAt(){return joinedAt;}
}
