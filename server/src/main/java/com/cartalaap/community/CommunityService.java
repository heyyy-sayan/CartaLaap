package com.cartalaap.community;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.BadRequestException;
import com.cartalaap.common.ForbiddenException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.notification.NotificationService;
import com.cartalaap.notification.NotificationType;
import com.cartalaap.realtime.RealtimeMessageGateway;
import com.cartalaap.user.CurrentUserService;
import com.cartalaap.user.User;
import com.cartalaap.user.UserRepository;

@Service
public class CommunityService {
    private final CommunityRepository communities; private final CommunityMemberRepository members;
    private final CommunityInviteRepository invites; private final CommunityMessageRepository messages;
    private final CommunityPollRepository polls; private final CommunityPollVoteRepository pollVotes;
    private final UserRepository users; private final CurrentUserService currentUsers;
    private final NotificationService notifications; private final RealtimeMessageGateway realtime;

    public CommunityService(CommunityRepository communities,CommunityMemberRepository members,
            CommunityInviteRepository invites,CommunityMessageRepository messages,CommunityPollRepository polls,
            CommunityPollVoteRepository pollVotes,UserRepository users,
            CurrentUserService currentUsers,NotificationService notifications,RealtimeMessageGateway realtime){
        this.communities=communities;this.members=members;this.invites=invites;this.messages=messages;this.polls=polls;this.pollVotes=pollVotes;this.users=users;
        this.currentUsers=currentUsers;this.notifications=notifications;this.realtime=realtime;
    }

    @Transactional(readOnly=true)
    public List<CommunityResponse> discover(Authentication auth){User current=currentUsers.optional(auth);return communities.findAllByOrderByCreatedAtDesc().stream().map(room->response(room,current)).toList();}

    @Transactional(readOnly=true)
    public List<CommunityResponse> mine(Authentication auth){User current=currentUsers.require(auth);return members.findByUser_IdOrderByJoinedAtDesc(current.getId()).stream().map(member->response(member.getCommunity(),current)).toList();}

    @Transactional
    public CommunityResponse create(CreateCommunityRequest request,Authentication auth){
        User creator=currentUsers.require(auth);String slug=slug(request.name());
        if(communities.existsBySlugIgnoreCase(slug))throw new BadRequestException("That community name is already taken");
        Community saved=communities.save(new Community(slug,clean(request.description()),creator));
        members.save(new CommunityMember(saved,creator,CommunityRole.OWNER));
        return response(saved,creator);
    }

    @Transactional(readOnly=true)
    public CommunityResponse get(String slug,Authentication auth){return response(find(slug),currentUsers.optional(auth));}

    @Transactional
    public CommunityResponse join(String slug,Authentication auth){Community room=find(slug);User current=currentUsers.require(auth);if(!members.existsByCommunity_IdAndUser_Id(room.getId(),current.getId()))members.save(new CommunityMember(room,current,CommunityRole.MEMBER));return response(room,current);}

    @Transactional
    public void leave(String slug,Authentication auth){Community room=find(slug);User current=currentUsers.require(auth);CommunityMember member=requireMember(room,current);if(member.getRole()==CommunityRole.OWNER)throw new BadRequestException("The owner cannot leave their community");members.deleteByCommunity_IdAndUser_Id(room.getId(),current.getId());}

    @Transactional(readOnly=true)
    public List<CommunityMemberResponse> memberList(String slug,Authentication auth){Community room=find(slug);requireMember(room,currentUsers.require(auth));return members.findByCommunity_IdOrderByJoinedAtAsc(room.getId()).stream().map(member->{var user=member.getUser();return new CommunityMemberResponse(user.getId(),user.getUsername(),user.getDisplayName(),user.getAvatarUrl(),member.getRole().name());}).toList();}

    @Transactional
    public CommunityInviteResponse invite(String slug,String username,Authentication auth){Community room=find(slug);User inviter=currentUsers.require(auth);requireMember(room,inviter);User invitee=users.findByUsernameIgnoreCase(username).orElseThrow(()->new NotFoundException("User not found"));if(invitee.getId().equals(inviter.getId()))throw new BadRequestException("You are already in this community");if(members.existsByCommunity_IdAndUser_Id(room.getId(),invitee.getId()))throw new BadRequestException("That user is already a member");CommunityInvite invitation=invites.findByCommunity_IdAndInvitee_Id(room.getId(),invitee.getId()).orElse(null);if(invitation==null)invitation=invites.save(new CommunityInvite(room,inviter,invitee));else invitation.renew(inviter);notifications.create(invitee,inviter,NotificationType.COMMUNITY_INVITE,inviter.getDisplayName()+" invited you to #"+room.getSlug(),room.getId());return inviteResponse(invitation,invitee);}

    @Transactional(readOnly=true)
    public List<CommunityInviteResponse> pendingInvites(Authentication auth){User current=currentUsers.require(auth);return invites.findByInvitee_IdAndStatusOrderByCreatedAtDesc(current.getId(),InviteStatus.PENDING).stream().map(invite->inviteResponse(invite,current)).toList();}

    @Transactional
    public CommunityResponse acceptInvite(Long id,Authentication auth){User current=currentUsers.require(auth);CommunityInvite invite=requireInvite(id,current);if(!members.existsByCommunity_IdAndUser_Id(invite.getCommunity().getId(),current.getId()))members.save(new CommunityMember(invite.getCommunity(),current,CommunityRole.MEMBER));invite.accept();return response(invite.getCommunity(),current);}

    @Transactional
    public void declineInvite(Long id,Authentication auth){CommunityInvite invite=requireInvite(id,currentUsers.require(auth));invite.decline();}

    @Transactional(readOnly=true)
    public List<CommunityMessageResponse> messageList(String slug,Authentication auth){Community room=find(slug);User current=currentUsers.require(auth);requireMember(room,current);return messages.findByCommunity_IdOrderByCreatedAtAsc(room.getId()).stream().map(message->CommunityMessageResponse.from(message,current.getId())).toList();}

    @Transactional
    public CommunityMessageResponse send(String slug,CommunityMessageRequest request,Authentication auth){
        Community room=find(slug);User current=currentUsers.require(auth);requireMember(room,current);
        String body=clean(request.body());String imageUrl=clean(request.imageUrl());
        if(body==null&&imageUrl==null&&request.poll()==null)throw new BadRequestException("Write a message, attach an image, or create a poll");
        CommunityMessage reply=null;
        if(request.replyToId()!=null){reply=messages.findDetailedById(request.replyToId()).orElseThrow(()->new NotFoundException("Reply message not found"));if(!reply.getCommunity().getId().equals(room.getId()))throw new BadRequestException("You can only reply to a message in this community");}
        CommunityMessage saved=messages.save(new CommunityMessage(room,current,body,imageUrl,reply));
        if(request.poll()!=null){
            var uniqueOptions=new LinkedHashMap<String,String>();
            request.poll().options().forEach(option->{String trimmed=option.trim();uniqueOptions.putIfAbsent(trimmed.toLowerCase(Locale.ROOT),trimmed);});
            if(uniqueOptions.size()<2)throw new BadRequestException("A poll needs at least two different options");
            CommunityPoll poll=new CommunityPoll(saved,request.poll().question().trim());int position=0;
            for(String option:uniqueOptions.values())poll.addOption(option,position++);
            CommunityPoll savedPoll=polls.saveAndFlush(poll);saved.attachPoll(savedPoll);
        }
        CommunityMessageResponse own=CommunityMessageResponse.from(saved,current.getId());
        broadcastMessage(room,saved,"community_message");return own;
    }

    @Transactional
    public CommunityMessageResponse vote(Long messageId,PollVoteRequest request,Authentication auth){
        CommunityMessage message=messages.findDetailedById(messageId).orElseThrow(()->new NotFoundException("Message not found"));
        User current=currentUsers.require(auth);requireMember(message.getCommunity(),current);
        CommunityPoll poll=message.getPoll();if(poll==null)throw new BadRequestException("This message does not contain a poll");
        CommunityPollOption option=poll.getOptions().stream().filter(item->item.getId().equals(request.optionId())).findFirst().orElseThrow(()->new BadRequestException("That option does not belong to this poll"));
        CommunityPollVote vote=pollVotes.findByPoll_IdAndUser_Id(poll.getId(),current.getId()).orElse(null);
        if(vote==null){vote=pollVotes.save(new CommunityPollVote(poll,current,option));poll.getVotes().add(vote);}else vote.choose(option);
        pollVotes.flush();broadcastMessage(message.getCommunity(),message,"community_message_updated");
        return CommunityMessageResponse.from(message,current.getId());
    }

    @Transactional
    public void deleteMessage(Long id,Authentication auth){CommunityMessage message=messages.findDetailedById(id).orElseThrow(()->new NotFoundException("Message not found"));User current=currentUsers.require(auth);if(!message.getSender().getId().equals(current.getId()))throw new ForbiddenException("You can only delete your own community messages");Community room=message.getCommunity();messages.delete(message);broadcast(room,Map.of("type","community_message_deleted","communitySlug",room.getSlug(),"messageId",id));}

    private void broadcast(Community room,Map<String,Object> event){members.findByCommunity_IdOrderByJoinedAtAsc(room.getId()).forEach(member->realtime.sendAfterCommit(member.getUser().getUsername(),event));}
    private void broadcastMessage(Community room,CommunityMessage message,String type){members.findByCommunity_IdOrderByJoinedAtAsc(room.getId()).forEach(member->realtime.sendAfterCommit(member.getUser().getUsername(),Map.of("type",type,"communitySlug",room.getSlug(),"message",CommunityMessageResponse.from(message,member.getUser().getId()))));}
    private Community find(String slug){return communities.findBySlugIgnoreCase(slug.replaceFirst("^#","")).orElseThrow(()->new NotFoundException("Community not found"));}
    private CommunityMember requireMember(Community room,User user){return members.findByCommunity_IdAndUser_Id(room.getId(),user.getId()).orElseThrow(()->new ForbiddenException("Join this community to enter its chat"));}
    private CommunityInvite requireInvite(Long id,User user){CommunityInvite invite=invites.findById(id).orElseThrow(()->new NotFoundException("Invitation not found"));if(!invite.getInvitee().getId().equals(user.getId())||invite.getStatus()!=InviteStatus.PENDING)throw new ForbiddenException("This invitation is not available");return invite;}
    private CommunityResponse response(Community room,User current){boolean joined=current!=null&&members.existsByCommunity_IdAndUser_Id(room.getId(),current.getId());var creator=room.getCreator();return new CommunityResponse(room.getId(),"#"+room.getSlug(),room.getSlug(),room.getDescription(),new CommunityResponse.Creator(creator.getId(),creator.getUsername(),creator.getDisplayName(),creator.getAvatarUrl()),members.countByCommunity_Id(room.getId()),joined,current!=null&&creator.getId().equals(current.getId()),room.getCreatedAt());}
    private CommunityInviteResponse inviteResponse(CommunityInvite invite,User current){var inviter=invite.getInviter();return new CommunityInviteResponse(invite.getId(),response(invite.getCommunity(),current),new CommunityInviteResponse.Inviter(inviter.getId(),inviter.getUsername(),inviter.getDisplayName(),inviter.getAvatarUrl()),invite.getCreatedAt());}
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private String slug(String value){String raw=value.trim().toLowerCase(Locale.ROOT).replaceFirst("^#","");if(!raw.matches("[a-z0-9][a-z0-9_-]{1,48}[a-z0-9]"))throw new BadRequestException("Community names must be 3–50 letters, numbers, underscores, or hyphens");return raw;}
}
