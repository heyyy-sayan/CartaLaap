package com.cartalaap.moment;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.ForbiddenException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.user.CurrentUserService;
import com.cartalaap.user.User;
import com.cartalaap.user.UserFollowRepository;
import com.cartalaap.notification.NotificationService;
import com.cartalaap.notification.NotificationType;

@Service
public class MomentService {
    private final MomentRepository moments;
    private final MomentViewRepository views;
    private final CurrentUserService currentUsers;
    private final UserFollowRepository follows;
    private final NotificationService notificationService;
    public MomentService(MomentRepository moments, MomentViewRepository views, CurrentUserService currentUsers, UserFollowRepository follows, NotificationService notificationService) {
        this.moments = moments; this.views = views; this.currentUsers = currentUsers; this.follows = follows; this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<MomentResponse> active(Authentication authentication) {
        User current = currentUsers.optional(authentication);
        List<MomentResponse> result = moments.findByExpiresAtAfterOrderByCreatedAtDesc(Instant.now()).stream().map(moment -> {
            boolean owned = current != null && moment.getAuthor().getId().equals(current.getId());
            boolean followed = current != null && !owned && follows.existsByFollower_IdAndFollowing_Id(current.getId(), moment.getAuthor().getId());
            boolean viewed = current != null && (owned || views.existsByMoment_IdAndViewer_Id(moment.getId(), current.getId()));
            return MomentResponse.from(moment, viewed, views.countByMoment_Id(moment.getId()), owned, followed);
        }).sorted(Comparator.comparingInt(this::priority).thenComparing(MomentResponse::createdAt, Comparator.reverseOrder())).toList();
        return result;
    }

    @Transactional
    public MomentResponse create(CreateMomentRequest request, Authentication authentication) {
        User current = currentUsers.require(authentication);
        Moment moment = moments.save(new Moment(current, request.imageUrl().trim(), clean(request.caption())));
        return MomentResponse.from(moment, true, 0, true, false);
    }

    @Transactional
    public MomentResponse view(Long id, Authentication authentication) {
        User current = currentUsers.require(authentication);
        Moment moment = activeMoment(id);
        boolean owned = moment.getAuthor().getId().equals(current.getId());
        if (!owned && views.insertIfMissing(id, current.getId()) > 0) notificationService.create(moment.getAuthor(), current, NotificationType.MOMENT_VIEW, current.getDisplayName() + " viewed your Moment", id);
        boolean followed = !owned && follows.existsByFollower_IdAndFollowing_Id(current.getId(), moment.getAuthor().getId());
        return MomentResponse.from(moment, true, views.countByMoment_Id(id), owned, followed);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        Moment moment = moments.findById(id).orElseThrow(() -> new NotFoundException("Moment not found"));
        User current = currentUsers.require(authentication);
        if (!moment.getAuthor().getId().equals(current.getId())) throw new ForbiddenException("You can only delete your own Moments");
        moments.delete(moment);
    }

    private Moment activeMoment(Long id) {
        Moment moment = moments.findById(id).orElseThrow(() -> new NotFoundException("Moment not found"));
        if (!moment.getExpiresAt().isAfter(Instant.now())) throw new NotFoundException("This Moment has expired");
        return moment;
    }
    private int priority(MomentResponse response) { return response.ownedByCurrentUser() ? 0 : response.followedAuthor() ? 1 : 2; }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
