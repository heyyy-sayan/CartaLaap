package com.cartalaap.community;
import java.time.Instant;
public record CommunityInviteResponse(Long id,CommunityResponse community,Inviter inviter,Instant createdAt){
    public record Inviter(Long id,String username,String displayName,String avatarUrl){}
}
