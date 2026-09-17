package com.documania.backend.audit;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private AuditLogRepository auditLogRepository;
    private UserAccountRepository userAccountRepository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        auditService = new AuditService(
            auditLogRepository,
            userAccountRepository
        );

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@documania.test",
                "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            )
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldStoreActorSnapshotAndSelectedValues() {
        UserAccount admin = new UserAccount(
            "admin@documania.test",
            "hash",
            new Role(RoleName.ADMIN, "Administrator"),
            "Admin",
            "User"
        );
        when(userAccountRepository.findByEmailIgnoreCase("admin@documania.test"))
            .thenReturn(Optional.of(admin));
        UUID staffPublicId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        auditService.record(
            AuditAction.STAFF_UPDATED,
            "STAFF",
            25L,
            staffPublicId,
            "staff@documania.test",
            Map.of("firstName", "Sara"),
            Map.of("firstName", "Sarah"),
            null
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();

        assertEquals(admin, auditLog.getActor());
        assertEquals("admin@documania.test", auditLog.getActorEmail());
        assertEquals("ADMIN", auditLog.getActorRole());
        assertEquals(AuditAction.STAFF_UPDATED, auditLog.getAction());
        assertEquals("STAFF", auditLog.getEntityType());
        assertEquals(25L, auditLog.getEntityId());
        assertEquals(staffPublicId, auditLog.getEntityPublicId());
        assertEquals("staff@documania.test", auditLog.getEntityLabel());
        assertEquals(Map.of("firstName", "Sara"), auditLog.getOldValues());
        assertEquals(Map.of("firstName", "Sarah"), auditLog.getNewValues());
        assertNull(auditLog.getReason());
    }

    @Test
    void shouldStoreSystemActorWithoutUserAccount() {
        auditService.recordSystem(AuditAction.SUBSCRIPTION_EXPIRED, "SUBSCRIPTION", 42L,
            UUID.fromString("22222222-2222-2222-2222-222222222222"), "Company",
            Map.of("status", "ACTIVE"), Map.of("status", "EXPIRED"), "Date de fin dépassée");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getActor());
        assertEquals("SYSTEM", captor.getValue().getActorEmail());
        assertEquals("SYSTEM", captor.getValue().getActorRole());
    }
}
