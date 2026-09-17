package com.documania.backend.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {
    Optional<AccountToken> findByTokenHashAndType(String tokenHash, AccountTokenType type);
    List<AccountToken> findAllByUserAccount_IdAndTypeAndUsedAtIsNull(Long userId, AccountTokenType type);
}
