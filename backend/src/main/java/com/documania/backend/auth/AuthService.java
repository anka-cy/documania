package com.documania.backend.auth;

import com.documania.backend.auth.dto.LoginResponse;
import com.documania.backend.common.exception.LoginLockedException;
import com.documania.backend.common.security.TokenHash;
import com.documania.backend.role.Permission;
import com.documania.backend.security.JwtService;
import com.documania.backend.security.RateLimitService;
import com.documania.backend.security.TokenBlacklistService;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RateLimitService rateLimitService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UserAccountRepository userAccountRepository,
        RefreshTokenRepository refreshTokenRepository,
        RateLimitService rateLimitService,
        TokenBlacklistService tokenBlacklistService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.rateLimitService = rateLimitService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public LoginResponse login(String email, String password, String clientIp) {
        // Throttle par IP (10 min), identique pour les comptes existants et inconnus :
        // le compte n'est jamais verrouillé et son existence n'est pas révélée.
        String throttleKey = "login:" + clientIp;

        if (rateLimitService.isLimited(throttleKey)) {
            throw new LoginLockedException(
                "Trop de tentatives de connexion. Réessayez plus tard ou réinitialisez votre mot de passe."
            );
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
            UserAccount account = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
            String token = jwtService.generateToken(authentication.getName(), authorities);
            String refreshToken = issueRefreshToken(account);
            rateLimitService.reset(throttleKey);
            return new LoginResponse(token, "Bearer", refreshToken, authentication.getName(), roleOf(authorities));
        } catch (AuthenticationException exception) {
            rateLimitService.record(throttleKey);
            throw exception;
        }
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository
            .findByTokenHashAndRevokedAtIsNull(TokenHash.hash(rawRefreshToken))
            .orElseThrow(() -> new BadCredentialsException("Le jeton de rafraîchissement est invalide"));
        LocalDateTime now = LocalDateTime.now();
        if (!stored.isUsableAt(now)) {
            throw new BadCredentialsException("Le jeton de rafraîchissement est expiré");
        }
        UserAccount account = stored.getUserAccount();
        if (!account.isEnabled() || !account.isEmailVerified()) {
            throw new BadCredentialsException("Le compte est indisponible");
        }
        stored.markRevoked(now);

        List<String> authorities = authoritiesOf(account);
        String token = jwtService.generateToken(account.getEmail(), authorities);
        String refreshToken = issueRefreshToken(account);
        return new LoginResponse(token, "Bearer", refreshToken, account.getEmail(), roleOf(authorities));
    }

    @Transactional
    public void logout(String rawRefreshToken, String authHeader) {
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(TokenHash.hash(rawRefreshToken))
            .ifPresent(token -> token.markRevoked(LocalDateTime.now()));
        blacklistAccessToken(authHeader);
    }

    // Blackliste le JWT d'accès jusqu'à son expiration naturelle (durée restante).
    private void blacklistAccessToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ") || tokenBlacklistService == null) {
            return;
        }
        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            return;
        }
        try {
            Instant expiration = jwtService.extractExpiration(token);
            Duration remaining = Duration.between(Instant.now(), expiration);
            if (remaining.isNegative() || remaining.isZero()) {
                return;
            }
            tokenBlacklistService.blacklist(token, remaining);
        } catch (JwtException | IllegalArgumentException exception) {
            // Jeton invalide : rien à blacklister
        }
    }

    private String issueRefreshToken(UserAccount account) {
        String rawToken = TokenHash.generateRaw();
        refreshTokenRepository.save(new RefreshToken(
            account, TokenHash.hash(rawToken), LocalDateTime.now().plus(REFRESH_TOKEN_TTL)
        ));
        return rawToken;
    }

    private List<String> authoritiesOf(UserAccount account) {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + account.getRole().getName().name());
        for (Permission permission : account.getRole().getPermissions()) {
            authorities.add(permission.getCode().name());
        }
        return authorities;
    }

    private String roleOf(List<String> authorities) {
        return authorities.stream()
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring(5))
            .findFirst()
            .orElse("CLIENT");
    }
}
