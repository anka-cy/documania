package com.documania.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenPurgeJobTest {

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenPurgeJob job;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        job = new RefreshTokenPurgeJob(refreshTokenRepository);
    }

    @Test
    void shouldDeleteTokensExpiredOrRevokedBeforeNow() {
        when(refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedAtNotNull(any(LocalDateTime.class)))
            .thenReturn(4);

        job.purgeExpiredTokens();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteByExpiresAtBeforeOrRevokedAtNotNull(cutoff.capture());
        assertFalse(cutoff.getValue().isAfter(LocalDateTime.now()));
        assertTrue(cutoff.getValue().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void shouldNotFailWhenNothingToDelete() {
        when(refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedAtNotNull(any(LocalDateTime.class)))
            .thenReturn(0);

        job.purgeExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiresAtBeforeOrRevokedAtNotNull(any(LocalDateTime.class));
    }
}
