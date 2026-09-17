package com.documania.backend.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Purge planifiée du journal d'audit : supprime périodiquement (par défaut
 * tous les dimanches à 03:00) les enregistrements antérieurs à la durée de
 * rétention (90 jours par défaut, configurable via {@code app.audit.retention-days}).
 */
@Component
public class AuditRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionJob.class);

    private final AuditLogRepository auditLogRepository;
    private final int retentionDays;

    public AuditRetentionJob(
        AuditLogRepository auditLogRepository,
        @Value("${app.audit.retention-days:90}") int retentionDays
    ) {
        this.auditLogRepository = auditLogRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.audit.retention.cron:0 0 3 * * SUN}")
    @Transactional
    public void purgeExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purge du journal d'audit : {} enregistrement(s) supprimé(s) avant {}", deleted, cutoff);
        }
    }
}
