package com.cartalaap.community;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface CommunityMessageRepository extends JpaRepository<CommunityMessage,Long>{
    @EntityGraph(attributePaths={"sender","replyTo","replyTo.sender","replyTo.poll","poll","poll.options","poll.votes","poll.votes.option"}) List<CommunityMessage> findByCommunity_IdOrderByCreatedAtAsc(Long communityId);
    @EntityGraph(attributePaths={"community","sender","replyTo","replyTo.sender","replyTo.poll","poll","poll.options","poll.votes","poll.votes.option"}) Optional<CommunityMessage> findDetailedById(Long id);
}
