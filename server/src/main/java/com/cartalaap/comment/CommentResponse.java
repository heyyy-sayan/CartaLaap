package com.cartalaap.comment;

import java.time.Instant;

public record CommentResponse(Long id, Author author, String body, String imageUrl, Instant createdAt, Instant updatedAt) {
    public static CommentResponse from(Comment comment) {
        var user = comment.getAuthor();
        return new CommentResponse(comment.getId(),
                new Author(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl()),
                comment.getBody(), comment.getImageUrl(), comment.getCreatedAt(), comment.getUpdatedAt());
    }

    public record Author(Long id, String username, String displayName, String avatarUrl) {
    }
}
