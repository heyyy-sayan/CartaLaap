package com.cartalaap.post;

import java.time.Instant;

public record PostResponse(
        Long id,
        Author author,
        String body,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt,
        long upvotes,
        long downvotes,
        long score,
        long commentCount,
        int currentUserVote,
        boolean ownedByCurrentUser) {

    public static PostResponse from(Post post, long upvotes, long downvotes, long commentCount,
            int currentUserVote, boolean ownedByCurrentUser) {
        var user = post.getAuthor();
        return new PostResponse(post.getId(),
                new Author(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl()),
                post.getBody(), post.getImageUrl(), post.getCreatedAt(), post.getUpdatedAt(),
                upvotes, downvotes, upvotes - downvotes, commentCount, currentUserVote, ownedByCurrentUser);
    }

    public record Author(Long id, String username, String displayName, String avatarUrl) {
    }
}
