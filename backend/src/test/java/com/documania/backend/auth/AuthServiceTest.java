package com.documania.backend.auth;

import com.documania.backend.common.exception.LoginLockedException;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.security.JwtService;
import com.documania.backend.security.RateLimitService;
import com.documania.backend.security.TokenBlacklistService;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private UserAccountRepository userAccountRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private RateLimitService rateLimitService;
    private TokenBlacklistService tokenBlacklistService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        userAccountRepository = mock(UserAccountRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        rateLimitService = mock(RateLimitService.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        authService = new AuthService(
            authenticationManager, jwtService, userAccountRepository, refreshTokenRepository,
            rateLimitService, tokenBlacklistService
        );
    }

    private UserAccount clientAccount() {
        UserAccount account = new UserAccount(
            "client@test.local", "password-hash", new Role(RoleName.CLIENT, "Client"), "John", "Doe"
        );
        account.markEmailVerified();
        return account;
    }

    @Test
    void shouldAuthenticateAndIssueTokenWithRefreshToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "client@test.local", null,
            List.of(new SimpleGrantedAuthority("ROLE_CLIENT"), new SimpleGrantedAuthority("CLIENT_READ")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local"))
            .thenReturn(Optional.of(clientAccount()));
        when(jwtService.generateToken("client@test.local", List.of("ROLE_CLIENT", "CLIENT_READ")))
            .thenReturn("jwt-token");
        when(rateLimitService.isLimited(anyString())).thenReturn(false);

        var response = authService.login("client@test.local", "correct-password", "127.0.0.1");

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("client@test.local", response.email());
        assertEquals("CLIENT", response.role());
        assertNotNull(response.refreshToken());
        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateToken("client@test.local", List.of("ROLE_CLIENT", "CLIENT_READ"));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(rateLimitService).reset(anyString());
    }

    @Test
    void shouldLetAuthenticationFailurePropagate() {
        when(rateLimitService.isLimited(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class,
            () -> authService.login("client@test.local", "wrong-password", "127.0.0.1"));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(rateLimitService).record(anyString());
    }

    @Test
    void shouldRejectLoginWhenThrottled() {
        when(rateLimitService.isLimited(anyString())).thenReturn(true);

        assertThrows(LoginLockedException.class,
            () -> authService.login("client@test.local", "correct-password", "127.0.0.1"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void shouldRotateTokensOnRefresh() {
        UserAccount account = clientAccount();
        RefreshToken stored = new RefreshToken(account, "stored-hash", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
            .thenReturn(Optional.of(stored));
        when(jwtService.generateToken("client@test.local", List.of("ROLE_CLIENT")))
            .thenReturn("new-jwt");

        var response = authService.refresh("raw-refresh-token");

        assertEquals("new-jwt", response.token());
        assertEquals("client@test.local", response.email());
        assertEquals("CLIENT", response.role());
        assertNotNull(response.refreshToken());
        assertNotNull(stored.getRevokedAt());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectUnknownOrAlreadyRevokedRefreshToken() {
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
            .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.refresh("raw-refresh-token"));
        verify(jwtService, never()).generateToken(anyString(), any());
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken stored = new RefreshToken(clientAccount(), "stored-hash", LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
            .thenReturn(Optional.of(stored));

        assertThrows(BadCredentialsException.class, () -> authService.refresh("raw-refresh-token"));
        assertNull(stored.getRevokedAt());
    }

    @Test
    void shouldRejectRefreshForDisabledAccount() {
        UserAccount account = clientAccount();
        account.disable();
        RefreshToken stored = new RefreshToken(account, "stored-hash", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
            .thenReturn(Optional.of(stored));

        assertThrows(BadCredentialsException.class, () -> authService.refresh("raw-refresh-token"));
    }

    @Test
    void shouldRevokeTokenOnLogout() {
        RefreshToken stored = new RefreshToken(clientAccount(), "stored-hash", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
            .thenReturn(Optional.of(stored));

        authService.logout("raw-refresh-token", null);

        assertNotNull(stored.getRevokedAt());
    }

    @Test
    void shouldBeIdempotentWhenLoggingOutUnknownToken() {
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
            .thenReturn(Optional.empty());

        authService.logout("raw-refresh-token", null);

        verify(refreshTokenRepository).findByTokenHashAndRevokedAtIsNull(anyString());
    }
}
