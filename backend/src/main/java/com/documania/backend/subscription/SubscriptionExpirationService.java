package com.documania.backend.subscription;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.notification.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SubscriptionExpirationService {
    private final SubscriptionRepository subscriptionRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationService notificationService;

    public SubscriptionExpirationService(SubscriptionRepository subscriptionRepository,
                                         AuditService auditService,
                                         EmailService emailService,
                                         EmailOutboxRepository emailOutboxRepository,
                                         NotificationService notificationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public int expireEndedSubscriptions(LocalDate today) {
        List<Subscription> ended = subscriptionRepository
            .findAllByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, today);
        for (Subscription subscription : ended) {
            subscription.expire();
            auditService.recordSystem(AuditAction.SUBSCRIPTION_EXPIRED, "SUBSCRIPTION",
                subscription.getId(), subscription.getPublicId(), subscription.getClientCompanyNameSnapshot(),
                Map.of("status", SubscriptionStatus.ACTIVE.name()),
                Map.of("status", SubscriptionStatus.EXPIRED.name(), "endDate", subscription.getEndDate()),
                "Date de fin dépassée");
            notificationService.notify(subscription.getClient().getUserAccount(),
                NotificationType.SUBSCRIPTION_EXPIRED, "Abonnement expiré",
                "Votre abonnement de " + subscription.getClientCompanyNameSnapshot()
                    + " est arrivé à expiration le " + subscription.getEndDate() + ".",
                "#/client/subscriptions");
        }
        return ended.size();
    }

    @Transactional
    public int sendExpiryReminders(LocalDate today) {
        List<Subscription> endingSoon = subscriptionRepository
            .findAllByStatusAndExpiryReminderSentFalseAndEndDateEquals(
                SubscriptionStatus.ACTIVE,
                today.plusDays(7)
            );
        for (Subscription subscription : endingSoon) {
            EmailService.EmailMessage message = emailService.buildSubscriptionExpiryReminderMessage(
                subscription.getClient().getUserAccount().getEmail(),
                subscription.getClientCompanyNameSnapshot(),
                subscription.getEndDate()
            );
            emailOutboxRepository.save(new EmailOutbox(
                message.recipient(), message.subject(), message.body()));
            subscription.markExpiryReminderSent();
            notificationService.notify(subscription.getClient().getUserAccount(),
                NotificationType.SUBSCRIPTION_EXPIRING, "Abonnement bientôt expiré",
                "Votre abonnement de " + subscription.getClientCompanyNameSnapshot()
                    + " expire le " + subscription.getEndDate() + ". Pensez à le renouveler.",
                "#/client/subscriptions");
        }
        return endingSoon.size();
    }
}