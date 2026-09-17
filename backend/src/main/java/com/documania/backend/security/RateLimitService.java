package com.documania.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limiteur de débit en mémoire (par clé : IP, IP+email, IP+jeton).
 * S'applique de façon uniforme aux comptes existants et inconnus afin de ne
 * pas divulguer l'existence d'un compte (pas d'oracle d'énumération).
 */
@Service
public class RateLimitService {

    private final int defaultMaxAttempts;
    private final Duration defaultWindow;

    /** key -> horodatages des tentatives récentes (ms). */
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public RateLimitService(
        @Value("${app.security.rate-limit.max-attempts:5}") int defaultMaxAttempts,
        @Value("${app.security.rate-limit.window:PT10M}") Duration defaultWindow
    ) {
        this.defaultMaxAttempts = defaultMaxAttempts;
        this.defaultWindow = defaultWindow;
    }

    /** Renvoie true si la clé a dépassé le seuil de tentatives sur la fenêtre courante. */
    public boolean isLimited(String key) {
        return isLimited(key, defaultMaxAttempts, defaultWindow);
    }

    /** Enregistre une tentative (échec) pour la clé. */
    public void record(String key) {
        record(key, defaultMaxAttempts, defaultWindow);
    }

    /** Renvoie true si la clé a dépassé le seuil sur la fenêtre passée en paramètre. */
    public boolean isLimited(String key, int maxAttempts, Duration window) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        Deque<Long> times = attempts.get(key);
        if (times == null) {
            return false;
        }
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < cutoff) {
                times.pollFirst();
            }
            return times.size() >= maxAttempts;
        }
    }

    /** Enregistre une tentative (échec) avec un seuil et une fenêtre explicites. */
    public void record(String key, int maxAttempts, Duration window) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        Deque<Long> times = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < cutoff) {
                times.pollFirst();
            }
            times.addLast(now);
        }
    }

    /** Réinitialise la clé (ex. après une connexion réussie). */
    public void reset(String key) {
        Deque<Long> times = attempts.get(key);
        if (times != null) {
            synchronized (times) {
                times.clear();
            }
        }
    }
}
