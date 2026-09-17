package com.documania.backend.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-signing";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600000);
    }

    @Test
    void shouldGenerateAndParseToken() {
        String token = jwtService.generateToken(
            "client@test.local", List.of("ROLE_CLIENT", "CLIENT_READ"));

        assertEquals("client@test.local", jwtService.extractUsername(token));
        assertTrue(jwtService.extractAuthorities(token).stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_CLIENT")));
        assertTrue(jwtService.extractAuthorities(token).stream()
            .anyMatch(authority -> authority.getAuthority().equals("CLIENT_READ")));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken("client@test.local", List.of("ROLE_CLIENT"));
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThrows(JwtException.class, () -> jwtService.extractUsername(tampered));
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() {
        JwtService other = new JwtService("another-test-secret-that-is-also-long-enough", 3600000);
        String token = other.generateToken("client@test.local", List.of("ROLE_CLIENT"));

        assertThrows(JwtException.class, () -> jwtService.extractUsername(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService alreadyExpired = new JwtService(SECRET, -1000);
        String token = alreadyExpired.generateToken("client@test.local", List.of("ROLE_CLIENT"));

        assertThrows(JwtException.class, () -> jwtService.extractUsername(token));
    }
}