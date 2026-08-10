package com.cartalaap.auth;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.ConflictException;
import com.cartalaap.user.User;
import com.cartalaap.user.UserRepository;
import com.cartalaap.user.UserResponse;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("That username is already taken");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("That email address is already registered");
        }

        User user = users.save(new User(username, email, passwordEncoder.encode(request.password()),
                request.displayName().trim()));
        return responseFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login().trim(), request.password()));
        User user = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(request.login().trim(), request.login().trim())
                .orElseThrow();
        return responseFor(user);
    }

    private AuthResponse responseFor(User user) {
        TokenService.IssuedToken token = tokenService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), UserResponse.from(user));
    }
}
