package com.cartalaap.community;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface CommunityInviteRepository extends JpaRepository<CommunityInvite,Long>{
    @EntityGraph(attributePaths={"community","community.creator","inviter"}) List<CommunityInvite> findByInvitee_IdAndStatusOrderByCreatedAtDesc(Long inviteeId,InviteStatus status);
    @EntityGraph(attributePaths={"community","inviter","invitee"}) Optional<CommunityInvite> findByCommunity_IdAndInvitee_Id(Long communityId,Long inviteeId);
}
