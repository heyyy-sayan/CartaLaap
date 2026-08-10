package com.cartalaap.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "author")
    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByTopic_SlugIgnoreCase(String topicSlug);

    @EntityGraph(attributePaths = "author")
    Page<Article> findByTopic_SlugIgnoreCaseOrderByCreatedAtDesc(String topicSlug, Pageable pageable);
}
