package com.documania.backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:query IS NULL OR :query = '' OR
               LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(a.entityLabel) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(a.reason) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(a.entityType) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:action IS NULL OR a.action = :action)
          AND (:entityType IS NULL OR :entityType = '' OR a.entityType = :entityType)
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> search(
        @Param("query") String query,
        @Param("action") AuditAction action,
        @Param("entityType") String entityType,
        Pageable pageable
    );

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
