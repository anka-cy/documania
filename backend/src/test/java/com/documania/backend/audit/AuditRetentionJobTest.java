package com.documania.backend.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditRetentionJobTest {

    @Test
    void shouldDeleteLogsOlderThanRetentionCutoff() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.deleteByCreatedAtBefore(any())).thenReturn(12);

        AuditRetentionJob job = new AuditRetentionJob(repository, 90);
        job.purgeExpiredLogs();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByCreatedAtBefore(captor.capture());
        LocalDateTime cutoff = captor.getValue();
        assertTrue(cutoff.isBefore(LocalDateTime.now().minusDays(89)));
        assertTrue(cutoff.isAfter(LocalDateTime.now().minusDays(91)));
    }

    @Test
    void shouldReturnDeletedCountWithoutErrorWhenNothingExpired() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.deleteByCreatedAtBefore(any())).thenReturn(0);

        AuditRetentionJob job = new AuditRetentionJob(repository, 90);
        job.purgeExpiredLogs();

        verify(repository).deleteByCreatedAtBefore(any());
    }

    @Test
    void shouldUseConfiguredRetentionDays() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.deleteByCreatedAtBefore(any())).thenReturn(1);

        AuditRetentionJob job = new AuditRetentionJob(repository, 30);
        job.purgeExpiredLogs();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByCreatedAtBefore(captor.capture());
        LocalDateTime cutoff = captor.getValue();
        assertTrue(cutoff.isBefore(LocalDateTime.now().minusDays(29)));
        assertTrue(cutoff.isAfter(LocalDateTime.now().minusDays(31)));
    }
}
