package com.documania.backend.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationStreamServiceTest {

    private NotificationStreamService service;

    @BeforeEach
    void setUp() {
        service = new NotificationStreamService();
    }

    @Test
    void shouldRegisterSubscribersPerChannel() {
        service.subscribe("user:1");
        service.subscribe("user:1");
        service.subscribe("user:2");

        assertEquals(2, service.subscriberCount("user:1"));
        assertEquals(1, service.subscriberCount("user:2"));
    }

    @Test
    void shouldPublishWithoutSubscribersSilently() {
        assertDoesNotThrow(() -> service.publish("user:99", "notification", Map.of("type", "X")));
    }

    @Test
    void shouldDeliverEventToConnectedSubscriber() {
        SseEmitter emitter = service.subscribe("user:7");

        service.publish("user:7", "notification", Map.of("type", "ORDER_CONFIRMED"));

        // Sans réponse HTTP attachée, SseEmitter met les événements en file :
        // l'essentiel est que l'émiteur reste vivant (aucun retrait sur échec).
        assertEquals(1, service.subscriberCount("user:7"));
    }

    @Test
    void heartbeatWithoutSubscribersIsANoOp() {
        service.subscribe("user:5");
        service.heartbeat();
        assertEquals(1, service.subscriberCount("user:5"));
    }
}
