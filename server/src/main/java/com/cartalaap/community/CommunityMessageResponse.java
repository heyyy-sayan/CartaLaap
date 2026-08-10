package com.cartalaap.community;
import java.time.Instant;
import java.util.List;

public record CommunityMessageResponse(Long id,Sender sender,String body,String imageUrl,ReplyPreview replyTo,Poll poll,Instant createdAt,boolean ownedByCurrentUser){
    public static CommunityMessageResponse from(CommunityMessage message,Long currentUserId){
        var sender=message.getSender();var reply=message.getReplyTo();var sourcePoll=message.getPoll();
        Poll poll=null;
        if(sourcePoll!=null){
            Long selected=sourcePoll.getVotes().stream().filter(vote->vote.getId().getUserId().equals(currentUserId)).map(vote->vote.getOption().getId()).findFirst().orElse(null);
            List<Poll.Option> options=sourcePoll.getOptions().stream().map(option->new Poll.Option(option.getId(),option.getText(),sourcePoll.getVotes().stream().filter(vote->vote.getOption().getId().equals(option.getId())).count())).toList();
            poll=new Poll(sourcePoll.getId(),sourcePoll.getQuestion(),options,selected,sourcePoll.getVotes().size());
        }
        return new CommunityMessageResponse(message.getId(),new Sender(sender.getId(),sender.getUsername(),sender.getDisplayName(),sender.getAvatarUrl()),message.getBody(),message.getImageUrl(),reply==null?null:new ReplyPreview(reply.getId(),reply.getSender().getDisplayName(),reply.getBody(),reply.getImageUrl(),reply.getPoll()==null?null:reply.getPoll().getQuestion()),poll,message.getCreatedAt(),sender.getId().equals(currentUserId));
    }
    public record Sender(Long id,String username,String displayName,String avatarUrl){}
    public record ReplyPreview(Long id,String senderName,String body,String imageUrl,String pollQuestion){}
    public record Poll(Long id,String question,List<Option> options,Long selectedOptionId,long totalVotes){public record Option(Long id,String text,long votes){}}
}
