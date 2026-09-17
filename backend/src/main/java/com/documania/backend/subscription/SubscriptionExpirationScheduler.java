package com.documania.backend.subscription;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SubscriptionExpirationScheduler {
    private final SubscriptionExpirationService expirationService;

    public SubscriptionExpirationScheduler(SubscriptionExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    @Scheduled(cron = "${app.subscriptions.expiration-cron:0 0 * * * *}")
    public void expireEndedSubscriptions() {
        expirationService.expireEndedSubscriptions(LocalDate.now());
    }

    @Scheduled(cron = "${app.subscriptions.reminder-cron:0 0 8 * * *}")
    public void sendExpiryReminders() {
        expirationService.sendExpiryReminders(LocalDate.now());
    }
}
