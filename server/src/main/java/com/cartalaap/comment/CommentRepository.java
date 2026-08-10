package com.cartalaap.comment;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "author")
    List<Comment> findByPost_IdOrderByCreatedAtAsc(Long postId);
    long countByPost_Id(Long postId);
}
