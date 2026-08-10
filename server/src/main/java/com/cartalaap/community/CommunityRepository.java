package com.cartalaap.community;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface CommunityRepository extends JpaRepository<Community,Long>{
    @EntityGraph(attributePaths="creator") List<Community> findAllByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths="creator") Optional<Community> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
}
