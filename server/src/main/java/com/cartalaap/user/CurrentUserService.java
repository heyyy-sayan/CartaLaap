package com.cartalaap.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.cartalaap.common.NotFoundException;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("Authenticated user was not found");
        }
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(authentication.getName(), authentication.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user was not found"));
    }

    public User optional(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(authentication.getName(), authentication.getName())
                .orElse(null);
    }
}
