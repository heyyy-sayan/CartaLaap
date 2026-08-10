package com.cartalaap.community;

import jakarta.validation.constraints.NotNull;

public record PollVoteRequest(@NotNull Long optionId) {}
