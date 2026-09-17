package com.documania.backend.ticket;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketTaskRepository extends JpaRepository<TicketTask, Long> {

    @EntityGraph(attributePaths = {"ticket"})
    List<TicketTask> findAllByTicket_IdOrderByPositionAscCreatedAtAsc(Long ticketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"ticket"})
    Optional<TicketTask> findByPublicIdAndTicket_Id(UUID publicId, Long ticketId);

    long countByTicket_Id(Long ticketId);
}
