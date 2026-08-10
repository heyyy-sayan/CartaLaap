package com.cartalaap.moment;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentRepository extends JpaRepository<Moment, Long> {
    @EntityGraph(attributePaths = "author")
    List<Moment> findByExpiresAtAfterOrderByCreatedAtDesc(Instant now);
}
