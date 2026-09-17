package com.documania.backend.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Registre SSE (text/event-stream) des flux de notifications en temps réel.
 *
 * <p>Un canal par destinataire ({@code user:<id>}) peut porter plusieurs émiteurs
 * simultanés (plusieurs onglets). Le registre est en mémoire JVM, cohérent avec la
 * contrainte mono-réplica déjà acceptée pour le blacklist et le rate limiting.
 * Un heartbeat (commentaire SSE toutes les 25 s) maintient la connexion vivante
 * derrière l'ingress Azure.</p>
 */
@Service
public class NotificationStreamService {

    /** Durée de vie d'un émiteur : le client SSE se reconnecte avant. */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, Map<Long, SseEmitter>> channels = new ConcurrentHashMap<>();
    private final AtomicLong emitterIds = new AtomicLong();

    public static String userChannel(Long userId) {
        return "user:" + userId;
    }

    public SseEmitter subscribe(String channel) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        long id = emitterIds.incrementAndGet();
        channels.computeIfAbsent(channel, ignored -> new ConcurrentHashMap<>()).put(id, emitter);
        emitter.onCompletion(() -> remove(channel, id));
        emitter.onTimeout(() -> remove(channel, id));
        emitter.onError(ignored -> remove(channel, id));
        return emitter;
    }

    /** Émet un événement sur tous les émiteurs d'un canal (no-op si personne). */
    public void publish(String channel, String eventName, Object payload) {
        Map<Long, SseEmitter> emitters = channels.get(channel);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException exception) {
                remove(channel, entry.getKey());
            }
        }
    }

    /** Nombre d'abonnés sur un canal (tests / observabilité). */
    public int subscriberCount(String channel) {
        Map<Long, SseEmitter> emitters = channels.get(channel);
        return emitters == null ? 0 : emitters.size();
    }

    /** Battement de cœur : commentaire SSE + nettoyage des canaux vides. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        channels.forEach((channel, emitters) -> {
            emitters.forEach((id, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException exception) {
                    remove(channel, id);
                }
            });
        });
        channels.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void remove(String channel, long id) {
        Map<Long, SseEmitter> emitters = channels.get(channel);
        if (emitters != null) {
            emitters.remove(id);
        }
    }
}
