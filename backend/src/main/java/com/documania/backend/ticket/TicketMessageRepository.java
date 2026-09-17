package com.documania.backend.ticket;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<TicketMessage> findAllByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
