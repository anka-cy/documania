package com.documania.backend.ticket;

import com.documania.backend.notification.NotificationStreamService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * Flux temps réel (SSE) des discussions de tickets : réutilise les canaux
 * génériques de NotificationStreamService — un canal « client » et un canal
 * « staff » par ticket. L'événement est un signal (« nouveau message ») :
 * chaque abonné recharge ensuite SA propre vue autorisée du ticket, le flux
 * ne transporte donc aucune donnée de conversation (pas de fuite de notes
 * internes via le stream).
 */
@Component
public class TicketStreamPublisher {

    public static final String EVENT_NEW_MESSAGE = "ticket-message";

    private final NotificationStreamService streamService;

    public TicketStreamPublisher(NotificationStreamService streamService) {
        this.streamService = streamService;
    }

    public static String clientChannel(UUID ticketPublicId) {
        return "ticket:" + ticketPublicId + ":client";
    }

    public static String staffChannel(UUID ticketPublicId) {
        return "ticket:" + ticketPublicId + ":staff";
    }

    public SseEmitter subscribeAsClient(UUID ticketPublicId) {
        return streamService.subscribe(clientChannel(ticketPublicId));
    }

    public SseEmitter subscribeAsStaff(UUID ticketPublicId) {
        return streamService.subscribe(staffChannel(ticketPublicId));
    }

    /** À appeler par le contrôleur APRÈS le commit du service transactionnel. */
    public void publishNewMessage(UUID ticketPublicId, UUID messageId) {
        Map<String, String> payload = Map.of("messageId", String.valueOf(messageId));
        streamService.publish(clientChannel(ticketPublicId), EVENT_NEW_MESSAGE, payload);
        streamService.publish(staffChannel(ticketPublicId), EVENT_NEW_MESSAGE, payload);
    }
}
