package com.cartalaap.message;

import java.time.Instant;

public record MessageResponse(Long id, Sender sender, String body, Instant createdAt, Instant readAt,
        boolean ownedByCurrentUser) {
    static MessageResponse from(DirectMessage message, Long currentUserId) {
        var sender = message.getSender();
        return new MessageResponse(message.getId(),
                new Sender(sender.getId(), sender.getUsername(), sender.getDisplayName(), sender.getAvatarUrl()),
                message.getBody(), message.getCreatedAt(), message.getReadAt(), sender.getId().equals(currentUserId));
    }
    public record Sender(Long id, String username, String displayName, String avatarUrl) {}
}
