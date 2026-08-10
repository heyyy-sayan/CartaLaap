package com.cartalaap.community;
import java.time.Instant;
public record CommunityResponse(Long id,String name,String slug,String description,Creator creator,long memberCount,boolean joinedByCurrentUser,boolean ownedByCurrentUser,Instant createdAt){
    public record Creator(Long id,String username,String displayName,String avatarUrl){}
}
