package com.cartalaap.vote;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.BadRequestException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.post.Post;
import com.cartalaap.post.PostRepository;
import com.cartalaap.user.CurrentUserService;
import com.cartalaap.user.User;
import com.cartalaap.notification.NotificationService;
import com.cartalaap.notification.NotificationType;

@Service
public class VoteService {
    private final PostVoteRepository votes;
    private final PostRepository posts;
    private final CurrentUserService currentUsers;
    private final NotificationService notificationService;

    public VoteService(PostVoteRepository votes, PostRepository posts, CurrentUserService currentUsers, NotificationService notificationService) {
        this.votes = votes;
        this.posts = posts;
        this.currentUsers = currentUsers;
        this.notificationService = notificationService;
    }

    @Transactional
    public VoteSummary vote(Long postId, VoteRequest request, Authentication authentication) {
        if (request.value() < -1 || request.value() > 1) {
            throw new BadRequestException("Vote must be -1, 0, or 1");
        }
        Post post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        User user = currentUsers.require(authentication);
        var existing = votes.findByPost_IdAndUser_Id(postId, user.getId());

        if (request.value() == 0) {
            existing.ifPresent(votes::delete);
        } else if (existing.isPresent()) {
            existing.get().setValue((short) request.value());
        } else {
            votes.save(new PostVote(post, user, (short) request.value()));
            notificationService.create(post.getAuthor(), user, NotificationType.VOTE, user.getDisplayName() + (request.value() > 0 ? " upvoted" : " downvoted") + " your post", post.getId());
        }
        votes.flush();
        return summary(postId, user);
    }

    @Transactional(readOnly = true)
    public VoteSummary summary(Long postId, User currentUser) {
        long upvotes = votes.countByPost_IdAndValue(postId, (short) 1);
        long downvotes = votes.countByPost_IdAndValue(postId, (short) -1);
        int currentVote = currentUser == null ? 0
                : votes.findByPost_IdAndUser_Id(postId, currentUser.getId()).map(PostVote::getValue).orElse((short) 0);
        return new VoteSummary(upvotes, downvotes, upvotes - downvotes, currentVote);
    }
}
