package com.cartalaap.comment;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.ForbiddenException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.common.BadRequestException;
import com.cartalaap.post.Post;
import com.cartalaap.post.PostRepository;
import com.cartalaap.user.CurrentUserService;
import com.cartalaap.user.User;
import com.cartalaap.notification.NotificationService;
import com.cartalaap.notification.NotificationType;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final PostRepository posts;
    private final CurrentUserService currentUsers;
    private final NotificationService notificationService;

    public CommentService(CommentRepository comments, PostRepository posts, CurrentUserService currentUsers, NotificationService notificationService) {
        this.comments = comments;
        this.posts = posts;
        this.currentUsers = currentUsers;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> forPost(Long postId) {
        if (!posts.existsById(postId)) {
            throw new NotFoundException("Post not found");
        }
        return comments.findByPost_IdOrderByCreatedAtAsc(postId).stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse create(Long postId, CommentRequest request, Authentication authentication) {
        Post post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        User author = currentUsers.require(authentication);
        String body = clean(request.body());
        String imageUrl = clean(request.imageUrl());
        requireContent(body, imageUrl);
        Comment saved = comments.save(new Comment(post, author, body, imageUrl));
        notificationService.create(post.getAuthor(), author, NotificationType.COMMENT, author.getDisplayName() + " commented on your post", post.getId());
        return CommentResponse.from(saved);
    }

    @Transactional
    public CommentResponse update(Long commentId, CommentRequest request, Authentication authentication) {
        Comment comment = comments.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        requireOwner(comment, currentUsers.require(authentication));
        String body = clean(request.body());
        String imageUrl = clean(request.imageUrl());
        requireContent(body, imageUrl);
        comment.update(body, imageUrl);
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long commentId, Authentication authentication) {
        Comment comment = comments.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        requireOwner(comment, currentUsers.require(authentication));
        comments.delete(comment);
    }

    private void requireOwner(Comment comment, User user) {
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only change your own comments");
        }
    }

    private void requireContent(String body, String imageUrl) {
        if (body == null && imageUrl == null) throw new BadRequestException("Write a comment or attach an image");
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
