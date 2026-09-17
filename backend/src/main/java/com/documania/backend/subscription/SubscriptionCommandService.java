package com.documania.backend.subscription;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.subscription.dto.SubscriptionResponse;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionCommandService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;

    public SubscriptionCommandService(SubscriptionRepository subscriptionRepository,
                                      SubscriptionPeriodRepository periodRepository,
                                      UserAccountRepository userAccountRepository,
                                      AuditService auditService,
                                      EmailService emailService,
                                      EmailOutboxRepository emailOutboxRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.periodRepository = periodRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
    }

    @Transactional
    public SubscriptionResponse cancel(String administratorEmail, UUID publicId, String reason) {
        Subscription subscription = subscriptionRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable : " + publicId));
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessRuleException("Seul un abonnement actif peut être annulé");
        }
        UserAccount administrator = userAccountRepository.findByEmailIgnoreCase(administratorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Compte administrateur introuvable"));
        String normalizedReason = reason.trim();
        subscription.cancel(normalizedReason, administrator, LocalDateTime.now());
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.SUBSCRIPTION_CANCELLED, "SUBSCRIPTION", saved.getId(),
            saved.getPublicId(), saved.getClientCompanyNameSnapshot(),
            Map.of("status", SubscriptionStatus.ACTIVE.name()),
            Map.of("status", SubscriptionStatus.CANCELLED.name(), "reason", normalizedReason,
                "cancelledAt", saved.getCancelledAt()), normalizedReason);
        List<SubscriptionPeriod> periods = periodRepository
            .findAllBySubscription_IdInOrderBySubscription_IdAscPeriodNumberAsc(List.of(saved.getId()));
        return SubscriptionMapper.toResponse(saved, periods);
    }

}
