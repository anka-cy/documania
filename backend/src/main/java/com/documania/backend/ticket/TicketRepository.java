package com.documania.backend.ticket;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = {"client", "order", "subscription", "assignedTo"})
    List<Ticket> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"client", "order", "subscription", "assignedTo"})
    List<Ticket> findAllByClient_IdOrderByCreatedAtDesc(Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"client", "order", "subscription", "assignedTo"})
    Optional<Ticket> findByPublicId(UUID publicId);

    /** Lecture seule (sans verrou) : utilisée par les vues de détail en transaction readOnly. */
    @EntityGraph(attributePaths = {"client", "order", "subscription", "assignedTo"})
    Optional<Ticket> findForReadByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"client", "order", "subscription", "assignedTo"})
    Optional<Ticket> findByPublicIdAndClient_Id(UUID publicId, Long clientId);

    /** Lecture seule (sans verrou) : utilisée par la fiche client en transaction readOnly. */
    @EntityGraph(attributePaths = {"client", "order", "subscription", "assignedTo"})
    Optional<Ticket> findForReadByPublicIdAndClient_Id(UUID publicId, Long clientId);

    @EntityGraph(attributePaths = {"client", "order", "subscription", "assignedTo"})
    Optional<Ticket> findByOrder_IdAndCategory(Long orderId, TicketCategory category);

    boolean existsByOrder_IdAndCategory(Long orderId, TicketCategory category);
}
