package com.cartalaap.message;

import java.time.Instant;

import com.cartalaap.user.User;

public record ConversationResponse(Long id, Participant participant, String lastMessage, Instant lastMessageAt,
        long unreadCount) {
    public record Participant(Long id, String username, String displayName, String avatarUrl) {
        static Participant from(User user) {
            return new Participant(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
        }
    }
}
