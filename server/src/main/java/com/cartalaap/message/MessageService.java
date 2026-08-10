package com.cartalaap.message;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.BadRequestException;
import com.cartalaap.common.ForbiddenException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.user.CurrentUserService;
import com.cartalaap.user.User;
import com.cartalaap.user.UserRepository;
import com.cartalaap.user.UserBlockRepository;
import com.cartalaap.notification.NotificationService;
import com.cartalaap.notification.NotificationType;
import com.cartalaap.realtime.RealtimeMessageGateway;

@Service
public class MessageService {
    private final ConversationRepository conversations;
    private final DirectMessageRepository messages;
    private final UserRepository users;
    private final CurrentUserService currentUsers;
    private final NotificationService notificationService;
    private final UserBlockRepository blocks;
    private final RealtimeMessageGateway realtime;

    public MessageService(ConversationRepository conversations, DirectMessageRepository messages,
            UserRepository users, CurrentUserService currentUsers, NotificationService notificationService,
            UserBlockRepository blocks, RealtimeMessageGateway realtime) {
        this.conversations = conversations; this.messages = messages; this.users = users; this.currentUsers = currentUsers;
        this.notificationService = notificationService;
        this.blocks = blocks;
        this.realtime = realtime;
    }

    @Transactional
    public ConversationResponse start(String username, Authentication authentication) {
        User current = currentUsers.require(authentication);
        User other = users.findByUsernameIgnoreCase(username).orElseThrow(() -> new NotFoundException("User not found"));
        if (current.getId().equals(other.getId())) throw new BadRequestException("You cannot message yourself");
        requireNoBlock(current, other);
        User one = current.getId() < other.getId() ? current : other;
        User two = current.getId() < other.getId() ? other : current;
        conversations.insertPairIfMissing(one.getId(), two.getId());
        Conversation conversation = conversations.findByUserOne_IdAndUserTwo_Id(one.getId(), two.getId())
                .orElseThrow(() -> new IllegalStateException("Conversation could not be created"));
        return toResponse(conversation, current);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> inbox(Authentication authentication) {
        User current = currentUsers.require(authentication);
        return conversations.findInbox(current.getId()).stream().map(c -> toResponse(c, current)).toList();
    }

    @Transactional
    public List<MessageResponse> thread(Long conversationId, Authentication authentication) {
        User current = currentUsers.require(authentication);
        Conversation conversation = requireParticipant(conversationId, current);
        List<DirectMessage> thread = messages.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());
        boolean changed = thread.stream().anyMatch(message -> !message.getSender().getId().equals(current.getId())
                && message.getReadAt() == null);
        thread.stream().filter(message -> !message.getSender().getId().equals(current.getId())).forEach(DirectMessage::markRead);
        if (changed) {
            User other = otherParticipant(conversation, current);
            realtime.sendAfterCommit(other.getUsername(), Map.of("type", "read_receipt",
                    "conversationId", conversation.getId(), "readerUsername", current.getUsername(),
                    "readAt", Instant.now().toString()));
        }
        return thread.stream().map(message -> MessageResponse.from(message, current.getId())).toList();
    }

    @Transactional
    public MessageResponse send(Long conversationId, SendMessageRequest request, Authentication authentication) {
        User current = currentUsers.require(authentication);
        Conversation conversation = requireParticipant(conversationId, current);
        User recipient = otherParticipant(conversation, current);
        requireNoBlock(current, recipient);
        DirectMessage message = messages.save(new DirectMessage(conversation, current, request.body().trim()));
        conversation.touch();
        notificationService.create(recipient, current, NotificationType.MESSAGE, current.getDisplayName() + " sent you a message", conversation.getId());
        MessageResponse senderResponse = MessageResponse.from(message, current.getId());
        realtime.sendAfterCommit(recipient.getUsername(), realtime.messageEvent(conversation.getId(),
                MessageResponse.from(message, recipient.getId())));
        realtime.sendAfterCommit(current.getUsername(), realtime.messageEvent(conversation.getId(), senderResponse));
        return senderResponse;
    }

    @Transactional
    public void delete(Long messageId, Authentication authentication) {
        DirectMessage message = messages.findById(messageId).orElseThrow(() -> new NotFoundException("Message not found"));
        User current = currentUsers.require(authentication);
        if (!message.getSender().getId().equals(current.getId())) throw new ForbiddenException("You can only delete your own messages");
        Conversation conversation = message.getConversation();
        User other = otherParticipant(conversation, current);
        messages.delete(message);
        Map<String, Object> event = Map.of("type", "message_deleted", "conversationId", conversation.getId(),
                "messageId", messageId);
        realtime.sendAfterCommit(other.getUsername(), event);
        realtime.sendAfterCommit(current.getUsername(), event);
    }

    private Conversation requireParticipant(Long id, User user) {
        Conversation conversation = conversations.findById(id).orElseThrow(() -> new NotFoundException("Conversation not found"));
        if (!conversation.getUserOne().getId().equals(user.getId()) && !conversation.getUserTwo().getId().equals(user.getId()))
            throw new ForbiddenException("You are not part of this conversation");
        return conversation;
    }

    private ConversationResponse toResponse(Conversation conversation, User current) {
        User other = otherParticipant(conversation, current);
        DirectMessage last = messages.findFirstByConversation_IdOrderByCreatedAtDesc(conversation.getId()).orElse(null);
        return new ConversationResponse(conversation.getId(), ConversationResponse.Participant.from(other),
                last == null ? null : last.getBody(), last == null ? conversation.getUpdatedAt() : last.getCreatedAt(),
                messages.countByConversation_IdAndSender_IdNotAndReadAtIsNull(conversation.getId(), current.getId()));
    }

    private User otherParticipant(Conversation conversation, User current) {
        return conversation.getUserOne().getId().equals(current.getId()) ? conversation.getUserTwo() : conversation.getUserOne();
    }

    private void requireNoBlock(User current, User other) {
        if (blocks.existsByBlocker_IdAndBlocked_Id(current.getId(), other.getId())
                || blocks.existsByBlocker_IdAndBlocked_Id(other.getId(), current.getId())) {
            throw new BadRequestException("You cannot message this user");
        }
    }
}
