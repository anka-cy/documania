package com.documania.backend.ticket;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    @EntityGraph(attributePaths = {"ticket", "message", "uploadedBy"})
    Optional<TicketAttachment> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = {"ticket"})
    List<TicketAttachment> findAllByMessage_IdOrderByCreatedAtAsc(Long messageId);
}
