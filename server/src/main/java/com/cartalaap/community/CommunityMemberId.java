package com.cartalaap.community;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.*;

@Embeddable
public class CommunityMemberId implements Serializable {
    @Column(name="community_id") private Long communityId;
    @Column(name="user_id") private Long userId;
    protected CommunityMemberId() {}
    public CommunityMemberId(Long communityId,Long userId){this.communityId=communityId;this.userId=userId;}
    @Override public boolean equals(Object o){return this==o||o instanceof CommunityMemberId that&&Objects.equals(communityId,that.communityId)&&Objects.equals(userId,that.userId);}
    @Override public int hashCode(){return Objects.hash(communityId,userId);}
}
