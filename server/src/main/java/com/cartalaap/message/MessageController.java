package com.cartalaap.message;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class MessageController {
    private final MessageService service;
    public MessageController(MessageService service) { this.service = service; }
    @GetMapping("/conversations") public List<ConversationResponse> inbox(Authentication auth) { return service.inbox(auth); }
    @PostMapping("/conversations/with/{username}") public ConversationResponse start(@PathVariable String username, Authentication auth) { return service.start(username, auth); }
    @GetMapping("/conversations/{id}/messages") public List<MessageResponse> thread(@PathVariable Long id, Authentication auth) { return service.thread(id, auth); }
    @PostMapping("/conversations/{id}/messages") @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request, Authentication auth) { return service.send(id, request, auth); }
    @DeleteMapping("/messages/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) { service.delete(id, auth); }
}
