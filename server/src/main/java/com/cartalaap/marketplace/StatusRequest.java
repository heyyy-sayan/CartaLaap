package com.cartalaap.marketplace;
import jakarta.validation.constraints.NotNull;
public record StatusRequest(@NotNull ListingStatus status){}
