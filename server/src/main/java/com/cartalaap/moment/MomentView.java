package com.cartalaap.moment;

import java.time.Instant;

import com.cartalaap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "moment_views")
public class MomentView {
    @EmbeddedId private MomentViewId id;
    @MapsId("momentId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "moment_id")
    private Moment moment;
    @MapsId("viewerId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "viewer_id")
    private User viewer;
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private Instant viewedAt;
    protected MomentView() {}
    public MomentView(Moment moment, User viewer) { this.id = new MomentViewId(moment.getId(), viewer.getId()); this.moment = moment; this.viewer = viewer; }
    @PrePersist void onCreate() { viewedAt = Instant.now(); }
}
