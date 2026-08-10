package com.cartalaap.topic;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityTopicRepository extends JpaRepository<CommunityTopic, Long> {
    List<CommunityTopic> findAllByOrderByNameAsc();
    Optional<CommunityTopic> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsByNameIgnoreCase(String name);
}
