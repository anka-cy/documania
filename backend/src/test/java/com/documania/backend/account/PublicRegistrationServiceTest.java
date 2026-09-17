package com.documania.backend.account;

import com.documania.backend.account.dto.RegisterClientRequest;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailOutboxStatus;
import com.documania.backend.email.EmailService;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.role.RoleRepository;
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

class PublicRegistrationServiceTest {

    private AccountTokenRepository tokenRepository;
    private UserAccountRepository userAccountRepository;
    private RoleRepository roleRepository;
    private ClientRepository clientRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private PublicRegistrationService service;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(AccountTokenRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        roleRepository = mock(RoleRepository.class);
        clientRepository = mock(ClientRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        service = new PublicRegistrationService(
            new AccountTokenService(tokenRepository, emailService, emailOutboxRepository),
            userAccountRepository, roleRepository, clientRepository,
            passwordEncoder, emailService
        );
    }

    private RegisterClientRequest validRequest() {
        return new RegisterClientRequest(
            "newclient@test.local", "Sara", "Amrani", "New Company",
            "+212600000000", "Rabat", "Tech",
            "A-strong-password-123", "A-strong-password-123"
        );
    }

    @Test
    void shouldCreateDisabledClientAccountAndQueueVerificationEmail() {
        Role role = new Role(RoleName.CLIENT, "Client");
        when(roleRepository.findByName(RoleName.CLIENT)).thenReturn(Optional.of(role));
        when(userAccountRepository.existsByEmailIgnoreCase("newclient@test.local")).thenReturn(false);
        when(passwordEncoder.encode("A-strong-password-123")).thenReturn("bcrypt-hash");
        when(tokenRepository.findAllByUserAccount_IdAndTypeAndUsedAtIsNull(any(), any())).thenReturn(List.of());
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        when(emailService.buildEmailVerificationMessage(
            eq("newclient@test.local"), rawTokenCaptor.capture(), any()
        )).thenReturn(new EmailService.EmailMessage(
            "newclient@test.local", "Confirmez votre adresse e-mail Documania", "corps"));

        service.register(validRequest());

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        UserAccount saved = accountCaptor.getValue();
        assertEquals("newclient@test.local", saved.getEmail());
        assertEquals("bcrypt-hash", saved.getPasswordHash());
        assertFalse(saved.isEnabled());
        assertFalse(saved.isEmailVerified());

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        assertEquals("New Company", clientCaptor.getValue().getCompanyName());

        ArgumentCaptor<AccountToken> tokenCaptor = ArgumentCaptor.forClass(AccountToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        AccountToken issued = tokenCaptor.getValue();
        assertEquals(AccountTokenType.EMAIL_VERIFICATION, issued.getType());
        assertTrue(issued.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
        assertTrue(issued.getExpiresAt().isBefore(LocalDateTime.now().plusHours(25)));
        assertNotEquals(rawTokenCaptor.getValue(), issued.getTokenHash());

        ArgumentCaptor<EmailOutbox> outboxCaptor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(emailOutboxRepository).save(outboxCaptor.capture());
        EmailOutbox queued = outboxCaptor.getValue();
        assertEquals("newclient@test.local", queued.getRecipient());
        assertEquals("Confirmez votre adresse e-mail Documania", queued.getSubject());
        assertEquals(EmailOutboxStatus.PENDING, queued.getStatus());
        assertFalse(rawTokenCaptor.getValue().isBlank());
    }

    @Test
    void shouldRejectDifferentPasswordConfirmation() {
        RegisterClientRequest request = new RegisterClientRequest(
            "newclient@test.local", "Sara", "Amrani", "New Company",
            null, null, null, "A-strong-password-123", "different-password"
        );
        assertThrows(BusinessRuleException.class, () -> service.register(request));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldRejectExistingEmail() {
        when(userAccountRepository.existsByEmailIgnoreCase("newclient@test.local")).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> service.register(validRequest()));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldVerifyEmailAndEnableAccount() {
        UserAccount account = new UserAccount(
            "newclient@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        AccountToken token = new AccountToken(
            account, "token-hash", AccountTokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(20));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));
        when(clientRepository.existsByUserAccount_IdAndArchived(account.getId(), false)).thenReturn(true);

        service.verifyEmail("raw-token");

        assertTrue(account.isEmailVerified());
        assertTrue(account.isEnabled());
        assertTrue(token.getUsedAt() != null);
        verify(userAccountRepository).save(account);
    }

    @Test
    void shouldRejectUnknownVerificationToken() {
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class, () -> service.verifyEmail("raw-token"));
    }

    @Test
    void shouldRejectExpiredOrUsedVerificationToken() {
        UserAccount account = new UserAccount(
            "newclient@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        AccountToken token = new AccountToken(
            account, "token-hash", AccountTokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(20));
        token.markUsed(LocalDateTime.now());
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class, () -> service.verifyEmail("raw-token"));
        assertFalse(account.isEmailVerified());
    }

    @Test
    void shouldRejectAlreadyVerifiedEmail() {
        UserAccount account = new UserAccount(
            "newclient@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        account.markEmailVerified();
        AccountToken token = new AccountToken(
            account, "token-hash", AccountTokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(20));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class, () -> service.verifyEmail("raw-token"));
    }

    @Test
    void shouldRejectVerificationForArchivedClient() {
        UserAccount account = new UserAccount(
            "newclient@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        AccountToken token = new AccountToken(
            account, "token-hash", AccountTokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(20));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));
        when(clientRepository.existsByUserAccount_IdAndArchived(account.getId(), false)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> service.verifyEmail("raw-token"));
        assertFalse(account.isEmailVerified());
    }
}