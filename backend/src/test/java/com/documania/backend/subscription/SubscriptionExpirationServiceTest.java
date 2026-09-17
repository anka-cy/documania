package com.documania.backend.subscription;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.CatalogService;
import com.documania.backend.client.Client;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.offer.Offer;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubscriptionExpirationServiceTest {

    private SubscriptionRepository repository;
    private AuditService auditService;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private com.documania.backend.notification.NotificationService notificationService;
    private SubscriptionExpirationService service;

    @BeforeEach
    void setUp() {
        repository = mock(SubscriptionRepository.class);
        auditService = mock(AuditService.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        notificationService = mock(com.documania.backend.notification.NotificationService.class);
        service = new SubscriptionExpirationService(repository, auditService, emailService, emailOutboxRepository,
            notificationService);
    }

    @Test void shouldExpireEndedActiveSubscriptionsAndAuditAsSystem() {
        LocalDate today = LocalDate.of(2026, 8, 14);
        Subscription subscription = subscription(today.minusMonths(1), today.minusDays(1));
        when(repository.findAllByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, today))
            .thenReturn(List.of(subscription));

        int count = service.expireEndedSubscriptions(today);

        assertEquals(1, count);
        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        verify(auditService).recordSystem(eq(AuditAction.SUBSCRIPTION_EXPIRED), eq("SUBSCRIPTION"),
            any(), any(), eq("Company"), any(), any(), eq("Date de fin dépassée"));
    }

    @Test void shouldDoNothingWhenNoSubscriptionHasEnded() {
        LocalDate today = LocalDate.of(2026, 8, 14);
        when(repository.findAllByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, today))
            .thenReturn(List.of());

        assertEquals(0, service.expireEndedSubscriptions(today));
        verifyNoInteractions(auditService);
    }

    @Test void shouldSendExpiryReminderSevenDaysBeforeAndMarkSent() {
        LocalDate today = LocalDate.of(2026, 8, 14);
        LocalDate endDate = today.plusDays(7);
        Subscription subscription = subscription(today.minusMonths(1), endDate);
        when(repository.findAllByStatusAndExpiryReminderSentFalseAndEndDateEquals(
            SubscriptionStatus.ACTIVE, endDate))
            .thenReturn(List.of(subscription));
        when(emailService.buildSubscriptionExpiryReminderMessage(
            eq("client@test.local"), eq("Company"), eq(endDate)))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        int count = service.sendExpiryReminders(today);

        assertEquals(1, count);
        assertTrue(subscription.isExpiryReminderSent());
        verify(emailOutboxRepository).save(any());
    }

    @Test void shouldDoNothingWhenNoSubscriptionEndsInSevenDays() {
        LocalDate today = LocalDate.of(2026, 8, 14);
        when(repository.findAllByStatusAndExpiryReminderSentFalseAndEndDateEquals(
            SubscriptionStatus.ACTIVE, today.plusDays(7)))
            .thenReturn(List.of());

        assertEquals(0, service.sendExpiryReminders(today));
        verifyNoInteractions(emailOutboxRepository);
    }

    private Subscription subscription(LocalDate start, LocalDate end) {
        UserAccount account = new UserAccount("client@test.local", "hash",
            new Role(RoleName.CLIENT, "Client"), "Test", "Client");
        Client client = new Client(account, "Company", null, null, null);
        CatalogService catalog = new CatalogService("Hosting", null, null);
        Offer offer = new Offer(catalog, "Pro", null, new BigDecimal("100.00"), 1, 5,
            start, end.plusYears(2));
        return new Subscription(client, offer, null, start, end);
    }
}