package com.cartalaap.moment;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MomentViewId implements Serializable {
    @Column(name = "moment_id") private Long momentId;
    @Column(name = "viewer_id") private Long viewerId;
    protected MomentViewId() {}
    public MomentViewId(Long momentId, Long viewerId) { this.momentId = momentId; this.viewerId = viewerId; }
    @Override public boolean equals(Object other) { return this == other || other instanceof MomentViewId that && Objects.equals(momentId, that.momentId) && Objects.equals(viewerId, that.viewerId); }
    @Override public int hashCode() { return Objects.hash(momentId, viewerId); }
}
