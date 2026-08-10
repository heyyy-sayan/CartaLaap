package com.cartalaap.vote;

public record VoteSummary(long upvotes, long downvotes, long score, int currentUserVote) {
}
