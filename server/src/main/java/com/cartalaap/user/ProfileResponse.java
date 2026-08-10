package com.cartalaap.user;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String location,
        String vehicleInterests,
        Instant joinedAt,
        long followers,
        long following,
        boolean followedByCurrentUser,
        boolean ownedByCurrentUser,
        boolean blockedByCurrentUser,
        boolean blocksCurrentUser) {
}
