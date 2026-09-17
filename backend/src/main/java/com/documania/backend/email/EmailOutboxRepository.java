package com.documania.backend.email;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    List<EmailOutbox> findAllByStatus(EmailOutboxStatus status);

    List<EmailOutbox> findAllByStatusAndNextRetryAtLessThanEqual(EmailOutboxStatus status, LocalDateTime time);
}