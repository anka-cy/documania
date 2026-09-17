package com.documania.backend.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketReadMarkRepository extends JpaRepository<TicketReadMark, Long> {

    Optional<TicketReadMark> findByTicket_IdAndUser_Id(Long ticketId, Long userId);

    /**
     * Nombre de messages non lus par ticket, en une requête. Un message est non
     * lu s'il est posté après la dernière lecture du couple (ticket, utilisateur)
     * ou sans trace de lecture ; les messages de l'utilisateur lui-même ne
     * comptent jamais.
     */
    @Query(value = """
        SELECT m.ticket_id, COUNT(*)
        FROM ticket_messages m
        LEFT JOIN ticket_read_marks r
               ON r.ticket_id = m.ticket_id AND r.user_id = :userId
        WHERE m.ticket_id IN (:ticketIds)
          AND m.author_id <> :userId
          AND (r.last_read_at IS NULL OR m.created_at > r.last_read_at)
        GROUP BY m.ticket_id
        """, nativeQuery = true)
    List<Object[]> countUnreadForTickets(@Param("userId") Long userId,
                                         @Param("ticketIds") List<Long> ticketIds);
}
