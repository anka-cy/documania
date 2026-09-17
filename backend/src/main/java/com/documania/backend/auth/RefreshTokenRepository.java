package com.documania.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    void deleteByUserAccount_Id(Long userId);

    @Modifying
    int deleteByExpiresAtBeforeOrRevokedAtNotNull(LocalDateTime cutoff);
}
