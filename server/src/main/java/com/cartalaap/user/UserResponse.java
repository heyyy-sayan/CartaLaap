package com.cartalaap.user;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        String location,
        String vehicleInterests,
        Instant joinedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                user.getBio(), user.getAvatarUrl(), user.getLocation(), user.getVehicleInterests(),
                user.getCreatedAt());
    }
}
