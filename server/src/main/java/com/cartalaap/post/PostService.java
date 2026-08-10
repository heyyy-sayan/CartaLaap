package com.cartalaap.post;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.comment.CommentRepository;
import com.cartalaap.common.ForbiddenException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.user.CurrentUserService;
import com.cartalaap.user.User;
import com.cartalaap.vote.VoteService;
import com.cartalaap.vote.VoteSummary;

@Service
public class PostService {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final VoteService voteService;
    private final CurrentUserService currentUsers;

    public PostService(PostRepository posts, CommentRepository comments, VoteService voteService,
            CurrentUserService currentUsers) {
        this.posts = posts;
        this.comments = comments;
        this.voteService = voteService;
        this.currentUsers = currentUsers;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> feed(int page, int size, Authentication authentication) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        User currentUser = currentUsers.optional(authentication);
        return PagedResponse.from(posts.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize))
                .map(post -> toResponse(post, currentUser)));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> followingFeed(int page, int size, Authentication authentication) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        User currentUser = currentUsers.require(authentication);
        return PagedResponse.from(posts.findFollowingFeed(currentUser, PageRequest.of(safePage, safeSize))
                .map(post -> toResponse(post, currentUser)));
    }

    @Transactional
    public PostResponse create(CreatePostRequest request, Authentication authentication) {
        User author = currentUsers.require(authentication);
        String imageUrl = request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim();
        return toResponse(posts.save(new Post(author, request.body().trim(), imageUrl)), author);
    }

    @Transactional
    public PostResponse update(Long postId, UpdatePostRequest request, Authentication authentication) {
        Post post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        User currentUser = currentUsers.require(authentication);
        requireOwner(post, currentUser);
        String imageUrl = request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim();
        post.update(request.body().trim(), imageUrl);
        return toResponse(post, currentUser);
    }

    @Transactional
    public void delete(Long postId, Authentication authentication) {
        Post post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        requireOwner(post, currentUsers.require(authentication));
        posts.delete(post);
    }

    private PostResponse toResponse(Post post, User currentUser) {
        VoteSummary votes = voteService.summary(post.getId(), currentUser);
        long commentCount = comments.countByPost_Id(post.getId());
        boolean owned = currentUser != null && post.getAuthor().getId().equals(currentUser.getId());
        return PostResponse.from(post, votes.upvotes(), votes.downvotes(), commentCount, votes.currentUserVote(), owned);
    }

    private void requireOwner(Post post, User user) {
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only change your own posts");
        }
    }
}
