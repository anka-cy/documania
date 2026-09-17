package com.documania.backend.subscription;

import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.CatalogService;
import com.documania.backend.client.Client;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.offer.Offer;
import com.documania.backend.order.CustomerOrder;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubscriptionCommandServiceTest {
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionPeriodRepository periodRepository;
    private UserAccountRepository userAccountRepository;
    private AuditService auditService;
    private SubscriptionCommandService service;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;

    @BeforeEach void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        periodRepository = mock(SubscriptionPeriodRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        auditService = mock(AuditService.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        service = new SubscriptionCommandService(subscriptionRepository, periodRepository,
            userAccountRepository, auditService, emailService, emailOutboxRepository);
    }

    @Test void shouldCancelActiveSubscriptionAndNormalizeReason() {
        UUID id = UUID.randomUUID();
        Subscription subscription = activeSubscription();
        org.springframework.test.util.ReflectionTestUtils.setField(subscription, "id", 42L);
        UserAccount admin = account(RoleName.ADMIN, "admin@test.local");
        when(subscriptionRepository.findByPublicId(id)).thenReturn(Optional.of(subscription));
        when(userAccountRepository.findByEmailIgnoreCase("admin@test.local")).thenReturn(Optional.of(admin));
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
        when(periodRepository.findAllBySubscription_IdInOrderBySubscription_IdAscPeriodNumberAsc(anyList()))
            .thenReturn(List.of());

        var response = service.cancel("admin@test.local", id, "  Résiliation demandée  ");

        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
        assertEquals("Résiliation demandée", subscription.getCancellationReason());
        assertEquals(admin, subscription.getCancelledBy());
        assertNotNull(subscription.getCancelledAt());
        assertEquals(SubscriptionStatus.CANCELLED, response.status());
        verify(auditService).record(any(), eq("SUBSCRIPTION"), any(), any(), any(), any(), any(),
            eq("Résiliation demandée"));
    }

    @Test void shouldRejectCancellingSubscriptionTwice() {
        UUID id = UUID.randomUUID();
        Subscription subscription = activeSubscription();
        subscription.cancel("Premier motif", account(RoleName.ADMIN, "admin@test.local"),
            java.time.LocalDateTime.now());
        when(subscriptionRepository.findByPublicId(id)).thenReturn(Optional.of(subscription));

        assertThrows(BusinessRuleException.class,
            () -> service.cancel("admin@test.local", id, "Deuxième motif"));
        verifyNoInteractions(userAccountRepository, periodRepository, auditService);
    }

    private Subscription activeSubscription() {
        Client client = new Client(account(RoleName.CLIENT, "client@test.local"), "Company", null, null, null);
        CatalogService catalog = new CatalogService("Hosting", null, null);
        Offer offer = new Offer(catalog, "Pro", null, new BigDecimal("100.00"), 1, 5,
            LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        CustomerOrder order = new CustomerOrder(client, offer, "ORD-TEST");
        return new Subscription(client, offer, order, LocalDate.now(), LocalDate.now().plusMonths(1).minusDays(1));
    }

    private UserAccount account(RoleName roleName, String email) {
        return new UserAccount(email, "hash", new Role(roleName, roleName.name()), "Test", "User");
    }
}
