package com.cartalaap.community;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CommunityMessageRequest(
        @Size(max=2000) String body,
        @Size(max=2048) String imageUrl,
        Long replyToId,
        @Valid PollRequest poll) {
    public record PollRequest(
            @NotBlank @Size(max=300) String question,
            @NotNull @Size(min=2,max=6) List<@NotBlank @Size(max=120) String> options) {}
}
