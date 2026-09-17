package com.documania.backend.security;

import com.documania.backend.common.security.TokenHash;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire en mémoire de la liste noire des jetons JWT révoqués (déconnexion).
 * Les jetons sont conservés jusqu'à leur date d'expiration naturelle puis purgés.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklist(String token, Duration remainingTtl) {
        if (token == null || token.isBlank() || remainingTtl == null || remainingTtl.isNegative() || remainingTtl.isZero()) {
            return;
        }
        Instant expiresAt = Instant.now().plus(remainingTtl);
        blacklistedTokens.put(TokenHash.hash(token), expiresAt);
        cleanExpired();
    }

    /** Vrai si le jeton est révoqué ; purgé si son expiration est atteinte. */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiresAt = blacklistedTokens.get(TokenHash.hash(token));
        if (expiresAt == null) {
            return false;
        }
        if (Instant.now().isAfter(expiresAt)) {
            blacklistedTokens.remove(TokenHash.hash(token));
            return false;
        }
        return true;
    }

    private void cleanExpired() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
