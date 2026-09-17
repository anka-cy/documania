package com.documania.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Purge planifiée des jetons de rafraîchissement : supprime périodiquement
 * (par défaut tous les jours à 03:30) les jetons expirés ou révoqués, afin
 * que la table ne croisse pas indéfiniment. Les jetons actifs ne sont
 * jamais touchés.
 */
@Component
public class RefreshTokenPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenPurgeJob.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenPurgeJob(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "${app.auth.refresh-token-purge.cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deleted = refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedAtNotNull(now);
        if (deleted > 0) {
            log.info("Purge des jetons de rafraîchissement : {} jeton(s) expiré(s)/révoqué(s) supprimé(s)", deleted);
        }
    }
}
