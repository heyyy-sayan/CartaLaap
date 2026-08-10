package com.cartalaap.community;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService service;
    public CommunityController(CommunityService service){this.service=service;}
    @GetMapping public List<CommunityResponse> discover(Authentication auth){return service.discover(auth);}
    @GetMapping("/mine") public List<CommunityResponse> mine(Authentication auth){return service.mine(auth);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CommunityResponse create(@Valid @RequestBody CreateCommunityRequest request,Authentication auth){return service.create(request,auth);}
    @GetMapping("/invites") public List<CommunityInviteResponse> invites(Authentication auth){return service.pendingInvites(auth);}
    @PostMapping("/invites/{id}/accept") public CommunityResponse accept(@PathVariable Long id,Authentication auth){return service.acceptInvite(id,auth);}
    @PostMapping("/invites/{id}/decline") @ResponseStatus(HttpStatus.NO_CONTENT) public void decline(@PathVariable Long id,Authentication auth){service.declineInvite(id,auth);}
    @GetMapping("/{slug}") public CommunityResponse get(@PathVariable String slug,Authentication auth){return service.get(slug,auth);}
    @PostMapping("/{slug}/join") public CommunityResponse join(@PathVariable String slug,Authentication auth){return service.join(slug,auth);}
    @DeleteMapping("/{slug}/leave") @ResponseStatus(HttpStatus.NO_CONTENT) public void leave(@PathVariable String slug,Authentication auth){service.leave(slug,auth);}
    @GetMapping("/{slug}/members") public List<CommunityMemberResponse> members(@PathVariable String slug,Authentication auth){return service.memberList(slug,auth);}
    @PostMapping("/{slug}/invites/{username}") public CommunityInviteResponse invite(@PathVariable String slug,@PathVariable String username,Authentication auth){return service.invite(slug,username,auth);}
    @GetMapping("/{slug}/messages") public List<CommunityMessageResponse> messages(@PathVariable String slug,Authentication auth){return service.messageList(slug,auth);}
    @PostMapping("/{slug}/messages") @ResponseStatus(HttpStatus.CREATED) public CommunityMessageResponse send(@PathVariable String slug,@Valid @RequestBody CommunityMessageRequest request,Authentication auth){return service.send(slug,request,auth);}
    @PostMapping("/messages/{id}/poll/vote") public CommunityMessageResponse vote(@PathVariable Long id,@Valid @RequestBody PollVoteRequest request,Authentication auth){return service.vote(id,request,auth);}
    @DeleteMapping("/messages/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteMessage(@PathVariable Long id,Authentication auth){service.deleteMessage(id,auth);}
}
