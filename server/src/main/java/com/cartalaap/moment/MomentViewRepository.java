package com.cartalaap.moment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MomentViewRepository extends JpaRepository<MomentView, MomentViewId> {
    boolean existsByMoment_IdAndViewer_Id(Long momentId, Long viewerId);
    long countByMoment_Id(Long momentId);
    @Modifying
    @Query(value = "INSERT IGNORE INTO moment_views (moment_id, viewer_id, viewed_at) VALUES (:momentId, :viewerId, CURRENT_TIMESTAMP(6))", nativeQuery = true)
    int insertIfMissing(@Param("momentId") Long momentId, @Param("viewerId") Long viewerId);
}
