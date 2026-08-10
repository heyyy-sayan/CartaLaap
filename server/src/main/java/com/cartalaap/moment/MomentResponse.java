package com.cartalaap.moment;

import java.time.Instant;

public record MomentResponse(Long id, Author author, String imageUrl, String caption, Instant createdAt,
        Instant expiresAt, boolean viewedByCurrentUser, long viewCount, boolean ownedByCurrentUser,
        boolean followedAuthor) {
    static MomentResponse from(Moment moment, boolean viewed, long viewCount, boolean owned, boolean followed) {
        var user = moment.getAuthor();
        return new MomentResponse(moment.getId(), new Author(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl()),
                moment.getImageUrl(), moment.getCaption(), moment.getCreatedAt(), moment.getExpiresAt(), viewed, viewCount, owned, followed);
    }
    public record Author(Long id, String username, String displayName, String avatarUrl) {}
}
