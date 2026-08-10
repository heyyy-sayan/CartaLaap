package com.cartalaap.notification;
import java.util.List;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.EntityGraph;import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<Notification,Long>{@EntityGraph(attributePaths="actor")List<Notification>findByRecipient_IdOrderByCreatedAtDesc(Long id,Pageable pageable);long countByRecipient_IdAndReadAtIsNull(Long id);}
