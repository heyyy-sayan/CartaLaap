package com.cartalaap.user;

import java.util.List;
import java.util.Comparator;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.BadRequestException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.post.PagedResponse;
import com.cartalaap.notification.NotificationService;
import com.cartalaap.notification.NotificationType;

@Service
public class ProfileService {
    private final UserRepository users;
    private final UserFollowRepository follows;
    private final UserBlockRepository blocks;
    private final CurrentUserService currentUsers;
    private final NotificationService notificationService;

    public ProfileService(UserRepository users, UserFollowRepository follows, UserBlockRepository blocks,
            CurrentUserService currentUsers, NotificationService notificationService) {
        this.users = users;
        this.follows = follows;
        this.blocks = blocks;
        this.currentUsers = currentUsers;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(String username, Authentication authentication) {
        return toProfile(find(username), currentUsers.optional(authentication));
    }

    @Transactional
    public UserResponse update(EditProfileRequest request, Authentication authentication) {
        User user = currentUsers.require(authentication);
        user.updateProfile(request.displayName().trim(), clean(request.bio()), clean(request.avatarUrl()),
                clean(request.location()), clean(request.vehicleInterests()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProfileResponse> search(String query, int page, int size, Authentication authentication) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 30));
        User current = currentUsers.optional(authentication);
        return PagedResponse.from(users.search(query == null ? "" : query.trim(), PageRequest.of(safePage, safeSize))
                .map(user -> toProfile(user, current)));
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> suggestions(int size, Authentication authentication) {
        int safeSize = Math.max(1, Math.min(size, 12));
        User current = currentUsers.optional(authentication);
        return users.findAll().stream()
                .filter(candidate -> current == null || !candidate.getId().equals(current.getId()))
                .filter(candidate -> current == null
                        || (!follows.existsByFollower_IdAndFollowing_Id(current.getId(), candidate.getId())
                                && !blocks.existsByBlocker_IdAndBlocked_Id(current.getId(), candidate.getId())
                                && !blocks.existsByBlocker_IdAndBlocked_Id(candidate.getId(), current.getId())))
                .map(candidate -> toProfile(candidate, current))
                .sorted(Comparator.comparingLong(ProfileResponse::followers).reversed()
                        .thenComparing(ProfileResponse::joinedAt, Comparator.reverseOrder()))
                .limit(safeSize)
                .toList();
    }

    @Transactional
    public ProfileResponse follow(String username, Authentication authentication) {
        User current = currentUsers.require(authentication);
        User target = find(username);
        if (current.getId().equals(target.getId())) {
            throw new BadRequestException("You cannot follow yourself");
        }
        requireNoBlock(current, target, "follow");
        if (!follows.existsByFollower_IdAndFollowing_Id(current.getId(), target.getId())) {
            follows.save(new UserFollow(current, target));
            notificationService.create(target, current, NotificationType.FOLLOW, current.getDisplayName() + " followed you", current.getId());
        }
        return toProfile(target, current);
    }

    @Transactional
    public ProfileResponse unfollow(String username, Authentication authentication) {
        User current = currentUsers.require(authentication);
        User target = find(username);
        follows.deleteByFollower_IdAndFollowing_Id(current.getId(), target.getId());
        follows.flush();
        return toProfile(target, current);
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> followers(String username, Authentication authentication) {
        User target = find(username);
        User current = currentUsers.optional(authentication);
        return follows.findByFollowing_IdOrderByCreatedAtDesc(target.getId(), PageRequest.of(0, 100)).stream()
                .map(UserFollow::getFollower).map(user -> toProfile(user, current)).toList();
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> following(String username, Authentication authentication) {
        User target = find(username);
        User current = currentUsers.optional(authentication);
        return follows.findByFollower_IdOrderByCreatedAtDesc(target.getId(), PageRequest.of(0, 100)).stream()
                .map(UserFollow::getFollowing).map(user -> toProfile(user, current)).toList();
    }

    @Transactional
    public ProfileResponse block(String username, Authentication authentication) {
        User current = currentUsers.require(authentication);
        User target = find(username);
        if (current.getId().equals(target.getId())) {
            throw new BadRequestException("You cannot block yourself");
        }
        if (!blocks.existsByBlocker_IdAndBlocked_Id(current.getId(), target.getId())) {
            blocks.save(new UserBlock(current, target));
        }
        follows.deleteByFollower_IdAndFollowing_Id(current.getId(), target.getId());
        follows.deleteByFollower_IdAndFollowing_Id(target.getId(), current.getId());
        follows.flush();
        return toProfile(target, current);
    }

    @Transactional
    public ProfileResponse unblock(String username, Authentication authentication) {
        User current = currentUsers.require(authentication);
        User target = find(username);
        blocks.deleteByBlocker_IdAndBlocked_Id(current.getId(), target.getId());
        blocks.flush();
        return toProfile(target, current);
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> blocked(Authentication authentication) {
        User current = currentUsers.require(authentication);
        return blocks.findByBlocker_IdOrderByCreatedAtDesc(current.getId()).stream()
                .map(UserBlock::getBlocked).map(user -> toProfile(user, current)).toList();
    }

    private ProfileResponse toProfile(User user, User current) {
        boolean owned = current != null && current.getId().equals(user.getId());
        boolean followed = current != null && !owned
                && follows.existsByFollower_IdAndFollowing_Id(current.getId(), user.getId());
        boolean blocked = current != null && !owned
                && blocks.existsByBlocker_IdAndBlocked_Id(current.getId(), user.getId());
        boolean blockedByUser = current != null && !owned
                && blocks.existsByBlocker_IdAndBlocked_Id(user.getId(), current.getId());
        return new ProfileResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getBio(),
                user.getAvatarUrl(), user.getLocation(), user.getVehicleInterests(), user.getCreatedAt(),
                follows.countByFollowing_Id(user.getId()), follows.countByFollower_Id(user.getId()), followed, owned,
                blocked, blockedByUser);
    }

    private void requireNoBlock(User current, User target, String action) {
        if (blocks.existsByBlocker_IdAndBlocked_Id(current.getId(), target.getId())
                || blocks.existsByBlocker_IdAndBlocked_Id(target.getId(), current.getId())) {
            throw new BadRequestException("You cannot " + action + " this user");
        }
    }

    private User find(String username) {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("User profile not found"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
