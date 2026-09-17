package com.documania.backend.auth;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenTest {

    private UserAccount account() {
        return new UserAccount(
            "client@test.local", "password-hash", new Role(RoleName.CLIENT, "Client"), "John", "Doe"
        );
    }

    @Test
    void shouldBeUsableWhenNotRevokedAndNotExpired() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken(account(), "stored-hash", now.plusHours(1));

        assertTrue(token.isUsableAt(now));
    }

    @Test
    void shouldBeExpiredOnceExpiryPassed() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken(account(), "stored-hash", now.minusMinutes(1));

        assertFalse(token.isUsableAt(now));
    }

    @Test
    void shouldBeRevokedAfterMarkRevoked() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken(account(), "stored-hash", now.plusHours(1));
        token.markRevoked(now);

        assertFalse(token.isUsableAt(now));
    }
}
