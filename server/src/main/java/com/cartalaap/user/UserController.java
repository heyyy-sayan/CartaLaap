package com.cartalaap.user;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cartalaap.post.PagedResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUserService currentUsers;
    private final ProfileService profiles;

    public UserController(CurrentUserService currentUsers, ProfileService profiles) {
        this.currentUsers = currentUsers;
        this.profiles = profiles;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(currentUsers.require(authentication));
    }

    @GetMapping("/me/blocked")
    public List<ProfileResponse> blocked(Authentication authentication) {
        return profiles.blocked(authentication);
    }

    @PatchMapping("/me")
    public UserResponse update(@Valid @RequestBody EditProfileRequest request, Authentication authentication) {
        return profiles.update(request, authentication);
    }

    @GetMapping("/search")
    public PagedResponse<ProfileResponse> search(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        return profiles.search(q, page, size, authentication);
    }

    @GetMapping("/suggestions")
    public List<ProfileResponse> suggestions(@RequestParam(defaultValue = "4") int size,
            Authentication authentication) {
        return profiles.suggestions(size, authentication);
    }

    @GetMapping("/{username}")
    public ProfileResponse profile(@PathVariable String username, Authentication authentication) {
        return profiles.profile(username, authentication);
    }

    @PostMapping("/{username}/follow")
    public ProfileResponse follow(@PathVariable String username, Authentication authentication) {
        return profiles.follow(username, authentication);
    }

    @DeleteMapping("/{username}/follow")
    public ProfileResponse unfollow(@PathVariable String username, Authentication authentication) {
        return profiles.unfollow(username, authentication);
    }

    @PostMapping("/{username}/block")
    public ProfileResponse block(@PathVariable String username, Authentication authentication) {
        return profiles.block(username, authentication);
    }

    @DeleteMapping("/{username}/block")
    public ProfileResponse unblock(@PathVariable String username, Authentication authentication) {
        return profiles.unblock(username, authentication);
    }

    @GetMapping("/{username}/followers")
    public List<ProfileResponse> followers(@PathVariable String username, Authentication authentication) {
        return profiles.followers(username, authentication);
    }

    @GetMapping("/{username}/following")
    public List<ProfileResponse> following(@PathVariable String username, Authentication authentication) {
        return profiles.following(username, authentication);
    }
}
