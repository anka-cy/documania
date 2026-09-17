package com.documania.backend.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    long countByStatus(SubscriptionStatus status);

    long countByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);

    boolean existsByCustomerOrder_Id(Long orderId);

    boolean existsByClient_IdAndStatus(Long clientId, SubscriptionStatus status);

    boolean existsByClient_IdAndStatusAndOffer_CatalogService_Id(
        Long clientId,
        SubscriptionStatus status,
        Long serviceId
    );

    @EntityGraph(attributePaths = {"client", "offer", "customerOrder", "cancelledBy"})
    List<Subscription> findAllByOrderByCreatedAtDesc();

    /** Variante paginée, les listes du portail staff étant non bornées. */
    @EntityGraph(attributePaths = {"client", "offer", "customerOrder", "cancelledBy"})
    Page<Subscription> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Recherche staff ; les périodes sont chargées séparément par le service (lot limité à la page). */
    @Query(value = """
        SELECT s FROM Subscription s
        LEFT JOIN FETCH s.client
        LEFT JOIN FETCH s.offer
        LEFT JOIN FETCH s.customerOrder
        LEFT JOIN FETCH s.cancelledBy
        WHERE (:status IS NULL OR s.status = :status)
          AND (:query IS NULL OR :query = '' OR
               LOWER(s.clientCompanyNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(s.offerNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(s.serviceNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')))
        """,
        countQuery = """
        SELECT COUNT(s) FROM Subscription s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:query IS NULL OR :query = '' OR
               LOWER(s.clientCompanyNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(s.offerNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(s.serviceNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Subscription> search(
        @Param("status") SubscriptionStatus status,
        @Param("query") String query,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"client", "offer", "customerOrder", "cancelledBy"})
    List<Subscription> findAllByClient_IdOrderByCreatedAtDesc(Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"client", "offer", "customerOrder", "cancelledBy"})
    Optional<Subscription> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"client", "offer", "customerOrder"})
    List<Subscription> findAllByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);

    @EntityGraph(attributePaths = {"client", "offer"})
    List<Subscription> findAllByStatusAndExpiryReminderSentFalseAndEndDateEquals(
        SubscriptionStatus status,
        LocalDate date
    );
}
