package com.cartalaap.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cartalaap.user.User;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = "author")
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("""
            SELECT post FROM Post post
            WHERE post.author = :currentUser
               OR post.author.id IN (
                    SELECT follow.following.id FROM UserFollow follow WHERE follow.follower = :currentUser
               )
            ORDER BY post.createdAt DESC
            """)
    Page<Post> findFollowingFeed(@Param("currentUser") User currentUser, Pageable pageable);
}
