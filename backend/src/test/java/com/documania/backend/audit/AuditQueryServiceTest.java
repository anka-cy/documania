package com.documania.backend.audit;

import com.documania.backend.audit.dto.AuditPageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditQueryServiceTest {

    @Test
    void shouldReturnRequestedAuditPageWithoutInternalIds() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditLog audit = mock(AuditLog.class);
        UUID entityPublicId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 14, 10, 30);

        when(audit.getActorEmail()).thenReturn("admin@documania.test");
        when(audit.getActorRole()).thenReturn("ADMIN");
        when(audit.getAction()).thenReturn(AuditAction.CLIENT_CREATED);
        when(audit.getEntityType()).thenReturn("CLIENT");
        when(audit.getEntityPublicId()).thenReturn(entityPublicId);
        when(audit.getEntityLabel()).thenReturn("Société Démo");
        when(audit.getOldValues()).thenReturn(Map.of());
        when(audit.getNewValues()).thenReturn(Map.of("companyName", "Société Démo"));
        when(audit.getCreatedAt()).thenReturn(createdAt);
        PageRequest request = PageRequest.of(1, 20);
        when(repository.search(isNull(), isNull(), isNull(), eq(request)))
            .thenReturn(new PageImpl<>(List.of(audit), request, 45));

        AuditPageResponse response = new AuditQueryService(repository).search(null, null, null, 1, 20);

        assertEquals(1, response.page());
        assertEquals(20, response.size());
        assertEquals(45, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(entityPublicId, response.items().getFirst().entityPublicId());
        assertEquals("admin@documania.test", response.items().getFirst().actorEmail());
        verify(repository).search(isNull(), isNull(), isNull(), eq(request));
    }

    @Test
    void shouldPassSearchCriteriaToRepository() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        PageRequest request = PageRequest.of(0, 20);
        when(repository.search("ORD-", AuditAction.ORDER_CONFIRMED, "ORDER", request))
            .thenReturn(new PageImpl<>(List.of(), request, 0));

        AuditPageResponse response = new AuditQueryService(repository)
            .search("  ORD-  ", AuditAction.ORDER_CONFIRMED, "ORDER", 0, 20);

        assertEquals(0, response.totalElements());
        verify(repository).search("ORD-", AuditAction.ORDER_CONFIRMED, "ORDER", request);
    }

    @Test
    void shouldNormalizeBlankCriteriaToNull() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        PageRequest request = PageRequest.of(0, 20);
        when(repository.search(isNull(), isNull(), isNull(), eq(request)))
            .thenReturn(new PageImpl<>(List.of(), request, 0));

        new AuditQueryService(repository).search("   ", null, "", 0, 20);

        verify(repository).search(isNull(), isNull(), isNull(), eq(request));
    }
}
