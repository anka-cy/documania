package com.documania.backend.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    long countByStatus(OrderStatus status);

    @Modifying
    @Query("UPDATE CustomerOrder o SET o.status = :newStatus, o.cancelledAt = :cancelledAt, o.updatedAt = :cancelledAt WHERE o.client.id = :clientId AND o.status = :targetStatus")
    int updateStatusForClientAndStatus(
        @Param("clientId") Long clientId,
        @Param("targetStatus") OrderStatus targetStatus,
        @Param("newStatus") OrderStatus newStatus,
        @Param("cancelledAt") LocalDateTime cancelledAt
    );

    boolean existsByClient_IdAndStatusAndOffer_CatalogService_Id(
        Long clientId,
        OrderStatus status,
        Long serviceId
    );

    @Query("SELECT COALESCE(SUM(o.priceSnapshot), 0) FROM CustomerOrder o WHERE o.status = :status")
    BigDecimal sumPriceSnapshotByStatus(@Param("status") OrderStatus status);
    @EntityGraph(attributePaths = {"client", "offer", "processedBy"})
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    /** Variante paginée, les listes du portail staff étant non bornées. */
    @EntityGraph(attributePaths = {"client", "offer", "processedBy"})
    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Recherche staff ; join fetch limité aux associations ManyToOne du mapper, la pagination reste en base. */
    @Query(value = """
        SELECT o FROM CustomerOrder o
        LEFT JOIN FETCH o.client
        LEFT JOIN FETCH o.offer
        LEFT JOIN FETCH o.processedBy
        WHERE (:status IS NULL OR o.status = :status)
          AND (:query IS NULL OR :query = '' OR
               LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(o.clientCompanyNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(o.offerNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(o.serviceNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')))
        """,
        countQuery = """
        SELECT COUNT(o) FROM CustomerOrder o
        WHERE (:status IS NULL OR o.status = :status)
          AND (:query IS NULL OR :query = '' OR
               LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(o.clientCompanyNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(o.offerNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(o.serviceNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<CustomerOrder> search(
        @Param("status") OrderStatus status,
        @Param("query") String query,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"client", "offer", "processedBy"})
    List<CustomerOrder> findAllByClient_IdOrderByCreatedAtDesc(Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"client", "offer", "processedBy"})
    Optional<CustomerOrder> findByPublicIdAndClient_Id(UUID publicId, Long clientId);

    @EntityGraph(attributePaths = {"client", "offer"})
    Optional<CustomerOrder> findForInvoiceByPublicIdAndClient_Id(UUID publicId, Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"client", "offer", "processedBy"})
    Optional<CustomerOrder> findByPublicId(UUID publicId);
}
