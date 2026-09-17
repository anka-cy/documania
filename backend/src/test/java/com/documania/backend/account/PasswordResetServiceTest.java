package com.documania.backend.account;

import com.documania.backend.auth.RefreshTokenRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailOutboxStatus;
import com.documania.backend.email.EmailService;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private AccountTokenRepository tokenRepository;
    private UserAccountRepository userAccountRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(AccountTokenRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        service = new PasswordResetService(
            new AccountTokenService(tokenRepository, emailService, emailOutboxRepository),
            userAccountRepository, refreshTokenRepository,
            passwordEncoder, emailService, emailOutboxRepository
        );
    }

    private UserAccount enabledAccount() {
        return new UserAccount(
            "client@test.local", "old-hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
    }

    @Test
    void shouldIssueResetTokenAndQueueEmailWhenAccountExists() {
        UserAccount account = enabledAccount();
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local")).thenReturn(Optional.of(account));
        when(tokenRepository.findAllByUserAccount_IdAndTypeAndUsedAtIsNull(any(), any())).thenReturn(List.of());

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        when(emailService.buildPasswordResetMessage(
            eq("client@test.local"), rawTokenCaptor.capture(), any()
        )).thenReturn(new EmailService.EmailMessage(
            "client@test.local", "Réinitialisez votre mot de passe Documania", "corps"));

        service.requestReset("client@test.local");

        ArgumentCaptor<AccountToken> tokenCaptor = ArgumentCaptor.forClass(AccountToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<EmailOutbox> outboxCaptor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(emailOutboxRepository).save(outboxCaptor.capture());

        AccountToken issued = tokenCaptor.getValue();
        assertEquals(AccountTokenType.PASSWORD_RESET, issued.getType());
        assertTrue(issued.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(29)));
        assertTrue(issued.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(31)));
        assertNotEquals(rawTokenCaptor.getValue(), issued.getTokenHash());

        EmailOutbox queued = outboxCaptor.getValue();
        assertEquals("client@test.local", queued.getRecipient());
        assertEquals("Réinitialisez votre mot de passe Documania", queued.getSubject());
        assertEquals(EmailOutboxStatus.PENDING, queued.getStatus());
        assertFalse(rawTokenCaptor.getValue().isBlank());
        verify(emailService, never()).send(any(), any(), any());
    }

    @Test
    void shouldInvalidatePreviousUnusedResetTokens() {
        UserAccount account = enabledAccount();
        AccountToken previous = new AccountToken(
            account, "old-hash", AccountTokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(20));
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local")).thenReturn(Optional.of(account));
        when(tokenRepository.findAllByUserAccount_IdAndTypeAndUsedAtIsNull(account.getId(), AccountTokenType.PASSWORD_RESET))
            .thenReturn(List.of(previous));
        when(emailService.buildPasswordResetMessage(eq("client@test.local"), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        service.requestReset("client@test.local");

        assertTrue(previous.getUsedAt() != null);
    }

    @Test
    void shouldDoNothingWhenAccountDoesNotExist() {
        when(userAccountRepository.findByEmailIgnoreCase("unknown@test.local")).thenReturn(Optional.empty());

        service.requestReset("unknown@test.local");

        verify(tokenRepository, never()).save(any());
        verify(emailOutboxRepository, never()).save(any());
    }

    @Test
    void shouldChangePasswordConsumeTokenAndRevokeRefreshTokens() {
        UserAccount account = enabledAccount();
        AccountToken token = new AccountToken(
            account, "hash", AccountTokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(20));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("A-new-secure-password")).thenReturn("new-bcrypt-hash");
        when(emailService.buildPasswordChangedMessage("client@test.local"))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        service.confirmReset("raw-token", "A-new-secure-password", "A-new-secure-password");

        assertEquals("new-bcrypt-hash", account.getPasswordHash());
        assertTrue(token.getUsedAt() != null);
        verify(refreshTokenRepository).deleteByUserAccount_Id(account.getId());
        verify(userAccountRepository).save(account);
        verify(emailOutboxRepository).save(any());
    }

    @Test
    void shouldRejectDifferentPasswordConfirmation() {
        assertThrows(BusinessRuleException.class,
            () -> service.confirmReset("token", "A-new-secure-password", "different-password"));
    }

    @Test
    void shouldRejectUnknownToken() {
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
            () -> service.confirmReset("raw-token", "A-new-secure-password", "A-new-secure-password"));
    }

    @Test
    void shouldRejectUsedOrExpiredToken() {
        UserAccount account = enabledAccount();
        AccountToken token = new AccountToken(
            account, "hash", AccountTokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(20));
        token.markUsed(LocalDateTime.now());
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class,
            () -> service.confirmReset("raw-token", "A-new-secure-password", "A-new-secure-password"));
    }

    @Test
    void shouldRejectDisabledAccount() {
        UserAccount account = enabledAccount();
        account.disable();
        AccountToken token = new AccountToken(
            account, "hash", AccountTokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(20));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class,
            () -> service.confirmReset("raw-token", "A-new-secure-password", "A-new-secure-password"));
    }
}