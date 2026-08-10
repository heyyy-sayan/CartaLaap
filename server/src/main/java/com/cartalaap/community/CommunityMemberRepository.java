package com.cartalaap.community;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface CommunityMemberRepository extends JpaRepository<CommunityMember,CommunityMemberId>{
    boolean existsByCommunity_IdAndUser_Id(Long communityId,Long userId);
    long countByCommunity_Id(Long communityId);
    void deleteByCommunity_IdAndUser_Id(Long communityId,Long userId);
    @EntityGraph(attributePaths={"community","community.creator"}) List<CommunityMember> findByUser_IdOrderByJoinedAtDesc(Long userId);
    @EntityGraph(attributePaths="user") List<CommunityMember> findByCommunity_IdOrderByJoinedAtAsc(Long communityId);
    Optional<CommunityMember> findByCommunity_IdAndUser_Id(Long communityId,Long userId);
}
