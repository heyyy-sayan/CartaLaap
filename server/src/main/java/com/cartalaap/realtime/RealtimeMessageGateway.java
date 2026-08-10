package com.cartalaap.realtime;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.cartalaap.message.Conversation;
import com.cartalaap.message.ConversationRepository;
import com.cartalaap.user.User;
import com.cartalaap.user.UserBlockRepository;
import com.cartalaap.user.UserRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class RealtimeMessageGateway extends TextWebSocketHandler implements SubProtocolCapable {
    private final ObjectMapper objectMapper;
    private final UserRepository users;
    private final ConversationRepository conversations;
    private final UserBlockRepository blocks;
    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public RealtimeMessageGateway(ObjectMapper objectMapper, UserRepository users,
            ConversationRepository conversations, UserBlockRepository blocks) {
        this.objectMapper = objectMapper;
        this.users = users;
        this.conversations = conversations;
        this.blocks = blocks;
    }

    @Override
    public List<String> getSubProtocols() {
        return List.of("cartalaap");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = username(session);
        boolean wasOffline = !sessions.containsKey(username);
        sessions.computeIfAbsent(username, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        send(session, Map.of("type", "presence_snapshot", "onlineUsers", sessions.keySet()));
        if (wasOffline) broadcast(Map.of("type", "presence", "username", username, "online", true));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> event = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
        if (!"typing".equals(event.get("type")) || !(event.get("conversationId") instanceof Number id)) return;
        boolean typing = Boolean.TRUE.equals(event.get("typing"));
        forwardTyping(username(session), id.longValue(), typing);
    }

    private void forwardTyping(String username, Long conversationId, boolean typing) {
        User current = users.findByUsernameIgnoreCase(username).orElse(null);
        Conversation conversation = conversations.findDetailedById(conversationId).orElse(null);
        if (current == null || conversation == null) return;
        User other;
        if (conversation.getUserOne().getId().equals(current.getId())) other = conversation.getUserTwo();
        else if (conversation.getUserTwo().getId().equals(current.getId())) other = conversation.getUserOne();
        else return;
        if (blocks.existsByBlocker_IdAndBlocked_Id(current.getId(), other.getId())
                || blocks.existsByBlocker_IdAndBlocked_Id(other.getId(), current.getId())) return;
        sendToUser(other.getUsername(), Map.of("type", "typing", "conversationId", conversationId,
                "username", current.getUsername(), "typing", typing));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = username(session);
        Set<WebSocketSession> userSessions = sessions.get(username);
        if (userSessions == null) return;
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(username, userSessions);
            broadcast(Map.of("type", "presence", "username", username, "online", false));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    public void sendAfterCommit(String username, Map<String, Object> event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { sendToUser(username, event); }
            });
        } else sendToUser(username, event);
    }

    public void sendToUser(String username, Map<String, Object> event) {
        Set<WebSocketSession> userSessions = sessions.get(username);
        if (userSessions == null) return;
        userSessions.forEach(session -> send(session, event));
    }

    private void broadcast(Map<String, Object> event) {
        sessions.values().forEach(userSessions -> userSessions.forEach(session -> send(session, event)));
    }

    private void send(WebSocketSession session, Object event) {
        if (!session.isOpen()) return;
        try {
            String payload = objectMapper.writeValueAsString(event);
            synchronized (session) { if (session.isOpen()) session.sendMessage(new TextMessage(payload)); }
        } catch (IOException ignored) {
        }
    }

    private String username(WebSocketSession session) {
        return session.getPrincipal() == null ? "" : session.getPrincipal().getName();
    }

    public Map<String, Object> messageEvent(Long conversationId, Object message) {
        return Map.of("type", "message", "conversationId", conversationId, "message", message,
                "sentAt", Instant.now().toString());
    }
}
