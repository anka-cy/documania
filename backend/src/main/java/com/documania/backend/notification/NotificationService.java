package com.documania.backend.notification;

import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.notification.dto.NotificationPageResponse;
import com.documania.backend.notification.dto.NotificationResponse;
import com.documania.backend.user.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamService streamService;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationStreamService streamService) {
        this.notificationRepository = notificationRepository;
        this.streamService = streamService;
    }

    @Transactional
    public Notification notify(
        UserAccount recipient,
        NotificationType type,
        String title,
        String message,
        String link
    ) {
        Notification saved = notificationRepository.save(
            new Notification(recipient, type, title, message, link)
        );
        publishStream(saved.getRecipient().getId(), type);
        return saved;
    }

    /**
     * Notifie plusieurs destinataires en une seule opération : une insertion
     * batch (saveAll) au lieu d'une insertion par destinataire. À utiliser
     * pour les diffusions staff/admin ; le compteur non-lus est recalculé
     * côté lecture (SELECT count(*)), aucun incrément n'est nécessaire.
     */
    @Transactional
    public void notifyAll(
        List<UserAccount> recipients,
        NotificationType type,
        String title,
        String message,
        String link
    ) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        List<Notification> notifications = recipients.stream()
            .map(recipient -> new Notification(recipient, type, title, message, link))
            .toList();
        notificationRepository.saveAll(notifications);
        recipients.forEach(recipient -> publishStream(recipient.getId(), type));
    }

    public List<NotificationResponse> listRecentForRecipient(Long recipientId) {
        return notificationRepository.findTop10ByRecipient_IdOrderByCreatedAtDesc(recipientId)
            .stream().map(NotificationResponse::from).toList();
    }

    public NotificationPageResponse listForRecipient(Long recipientId, Pageable pageable) {
        Page<Notification> page = notificationRepository
            .findByRecipient_IdOrderByCreatedAtDesc(recipientId, pageable);
        List<NotificationResponse> items = page.getContent().stream()
            .map(NotificationResponse::from)
            .toList();
        return new NotificationPageResponse(
            items, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages()
        );
    }

    public long countUnread(Long recipientId) {
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public NotificationResponse markRead(Long recipientId, UUID publicId) {
        Notification notification = notificationRepository
            .findByPublicIdAndRecipient_Id(publicId, recipientId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable : " + publicId));
        notification.markRead();
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(Long recipientId) {
        return notificationRepository.markAllReadForRecipient(recipientId);
    }

    /**
     * Signale le flux SSE après le commit de la transaction : le client recharge son
     * compteur à la réception et ne doit jamais voir un état non encore visible en base.
     * Hors transaction (tests unitaires), la publication est immédiate.
     */
    private void publishStream(Long recipientId, NotificationType type) {
        String channel = NotificationStreamService.userChannel(recipientId);
        Map<String, String> payload = Map.of("type", type.name());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    streamService.publish(channel, "notification", payload);
                }
            });
        } else {
            streamService.publish(channel, "notification", payload);
        }
    }
}
