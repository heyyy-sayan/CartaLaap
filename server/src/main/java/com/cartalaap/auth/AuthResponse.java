package com.cartalaap.auth;

import java.time.Instant;

import com.cartalaap.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
}
