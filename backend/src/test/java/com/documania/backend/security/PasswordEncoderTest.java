package com.documania.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder =
        new SecurityBeansConfig().passwordEncoder();

    @Test
    void shouldHashAndVerifyPassword() {
        String plainPassword = "MotDePasseTest123!";

        String passwordHash = passwordEncoder.encode(plainPassword);

        assertNotEquals(plainPassword, passwordHash);
        assertTrue(passwordEncoder.matches(plainPassword, passwordHash));
        assertFalse(passwordEncoder.matches("MauvaisMotDePasse", passwordHash));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        String plainPassword = "MotDePasseTest123!";

        String firstHash = passwordEncoder.encode(plainPassword);
        String secondHash = passwordEncoder.encode(plainPassword);

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordEncoder.matches(plainPassword, firstHash));
        assertTrue(passwordEncoder.matches(plainPassword, secondHash));
    }
}
