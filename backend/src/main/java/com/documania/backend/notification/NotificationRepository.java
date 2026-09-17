package com.documania.backend.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop10ByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    Page<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);

    Optional<Notification> findByPublicIdAndRecipient_Id(UUID publicId, Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :recipientId AND n.readAt IS NULL")
    int markAllReadForRecipient(@Param("recipientId") Long recipientId);
}
