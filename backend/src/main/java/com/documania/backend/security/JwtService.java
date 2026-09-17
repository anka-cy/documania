package com.documania.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(String secret, long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, List<String> authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(username)
            .claim("authorities", authorities)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMs)))
            .signWith(key)
            .compact();
    }

    public String extractUsername(String token) {
        return claimsOf(token).getSubject();
    }

    /** Date d'expiration du jeton (utile pour le TTL de la blacklist de déconnexion). */
    public Instant extractExpiration(String token) {
        return claimsOf(token).getExpiration().toInstant();
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        Object raw = claimsOf(token).get("authorities");
        if (raw instanceof List<?> list) {
            return list.stream()
                .map(String::valueOf)
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    private Claims claimsOf(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}