package com.documania.backend.account;

import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
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

import java.util.Optional;
import java.util.List;
import java.util.UUID;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountActivationServiceTest {

    private AccountTokenRepository tokenRepository;
    private ClientRepository clientRepository;
    private UserAccountRepository userAccountRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private AccountActivationService service;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(AccountTokenRepository.class);
        clientRepository = mock(ClientRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        service = new AccountActivationService(
            new AccountTokenService(tokenRepository, emailService, emailOutboxRepository),
            clientRepository,
            userAccountRepository,
            passwordEncoder,
            emailService
        );
    }

    @Test
    void shouldIssueAHashedTokenAndQueueActivationEmail() {
        UUID clientId = UUID.randomUUID();
        Client client = inactiveClient();
        when(clientRepository.findByPublicIdAndArchived(clientId, false)).thenReturn(Optional.of(client));
        when(tokenRepository.findAllByUserAccount_IdAndTypeAndUsedAtIsNull(any(), any())).thenReturn(List.of());

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        when(emailService.buildAccountActivationMessage(
            org.mockito.ArgumentMatchers.eq("client@test.local"),
            rawTokenCaptor.capture(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(new EmailService.EmailMessage(
            "client@test.local", "Activez votre compte Documania", "corps"));

        var response = service.issueForClient(clientId);

        ArgumentCaptor<AccountToken> captor = ArgumentCaptor.forClass(AccountToken.class);
        verify(tokenRepository).save(captor.capture());
        ArgumentCaptor<EmailOutbox> outboxCaptor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(emailOutboxRepository).save(outboxCaptor.capture());

        EmailOutbox queued = outboxCaptor.getValue();
        assertEquals("client@test.local", queued.getRecipient());
        assertEquals("Activez votre compte Documania", queued.getSubject());
        assertEquals("corps", queued.getBody());
        assertEquals(EmailOutboxStatus.PENDING, queued.getStatus());
        assertFalse(rawTokenCaptor.getValue().isBlank());
        assertNotEquals(rawTokenCaptor.getValue(), captor.getValue().getTokenHash());
        assertTrue(response.expiresAt().isAfter(java.time.LocalDateTime.now().plusHours(23)));
        verify(emailService, never()).send(any(), any(), any());
    }

    @Test
    void shouldActivateAccountAndConsumeToken() {
        UserAccount account = inactiveClient().getUserAccount();
        AccountToken token = new AccountToken(account, "hash", AccountTokenType.PASSWORD_SETUP,
            java.time.LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));
        when(clientRepository.existsByUserAccount_IdAndArchived(any(), org.mockito.ArgumentMatchers.eq(false)))
            .thenReturn(true);
        when(passwordEncoder.encode("A-secure-password-123")).thenReturn("bcrypt-hash");

        service.activate("raw-token", "A-secure-password-123", "A-secure-password-123");

        assertTrue(account.isEnabled());
        assertTrue(account.isEmailVerified());
        assertTrue(token.getUsedAt() != null);
        verify(userAccountRepository).save(account);
    }

    @Test
    void shouldRejectDifferentPasswordConfirmation() {
        assertThrows(BusinessRuleException.class,
            () -> service.activate("token", "A-secure-password-123", "different-password"));
    }

    @Test
    void shouldRejectReusingAToken() {
        AccountToken token = new AccountToken(inactiveClient().getUserAccount(), "hash",
            AccountTokenType.PASSWORD_SETUP, java.time.LocalDateTime.now().plusHours(1));
        token.markUsed(java.time.LocalDateTime.now());
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class,
            () -> service.activate("token", "A-secure-password-123", "A-secure-password-123"));
    }

    @Test
    void shouldRejectActivationAfterClientWasArchived() {
        AccountToken token = new AccountToken(inactiveClient().getUserAccount(), "hash",
            AccountTokenType.PASSWORD_SETUP, java.time.LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));
        when(clientRepository.existsByUserAccount_IdAndArchived(any(), org.mockito.ArgumentMatchers.eq(false)))
            .thenReturn(false);

        assertThrows(BusinessRuleException.class,
            () -> service.activate("token", "A-secure-password-123", "A-secure-password-123"));
    }

    @Test
    void shouldSendStaffInvitationAndQueueEmail() {
        UserAccount staffAccount = staffAccount();
        when(tokenRepository.findAllByUserAccount_IdAndTypeAndUsedAtIsNull(any(), any())).thenReturn(List.of());
        when(emailService.buildAccountActivationMessage(eq("staff@test.local"), any(), any()))
            .thenReturn(new EmailService.EmailMessage(
                "staff@test.local", "Activez votre compte Documania", "corps"));

        service.sendStaffInvitation(staffAccount);

        ArgumentCaptor<AccountToken> tokenCaptor = ArgumentCaptor.forClass(AccountToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertEquals(AccountTokenType.PASSWORD_SETUP, tokenCaptor.getValue().getType());
        ArgumentCaptor<EmailOutbox> outboxCaptor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(emailOutboxRepository).save(outboxCaptor.capture());
        assertEquals("staff@test.local", outboxCaptor.getValue().getRecipient());
    }

    @Test
    void shouldRejectStaffInvitationForAlreadyVerifiedAccount() {
        UserAccount staffAccount = staffAccount();
        staffAccount.markEmailVerified();

        assertThrows(BusinessRuleException.class, () -> service.sendStaffInvitation(staffAccount));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void shouldIssueStaffInvitationByPublicId() {
        UUID staffId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserAccount staffAccount = staffAccount();
        when(userAccountRepository.findByPublicIdAndRole_Name(staffId, RoleName.STAFF))
            .thenReturn(Optional.of(staffAccount));
        when(tokenRepository.findAllByUserAccount_IdAndTypeAndUsedAtIsNull(any(), any())).thenReturn(List.of());
        when(emailService.buildAccountActivationMessage(eq("staff@test.local"), any(), any()))
            .thenReturn(new EmailService.EmailMessage(
                "staff@test.local", "Activez votre compte Documania", "corps"));

        service.issueForStaff(staffId);

        verify(emailOutboxRepository).save(any());
    }

    @Test
    void shouldActivateStaffAccountWithoutClientRow() {
        UserAccount staffAccount = staffAccount();
        AccountToken token = new AccountToken(staffAccount, "hash", AccountTokenType.PASSWORD_SETUP,
            java.time.LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("A-secure-password-123")).thenReturn("bcrypt-hash");

        service.activate("raw-token", "A-secure-password-123", "A-secure-password-123");

        assertTrue(staffAccount.isEnabled());
        assertTrue(staffAccount.isEmailVerified());
        assertTrue(token.getUsedAt() != null);
        verify(userAccountRepository).save(staffAccount);
        verifyNoInteractions(clientRepository);
    }

    private UserAccount staffAccount() {
        return new UserAccount("staff@test.local", "old-hash",
            new Role(RoleName.STAFF, "Personnel"), "Sara", "Amrani");
    }

    private Client inactiveClient() {
        UserAccount account = new UserAccount("client@test.local", "old-hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        account.disable();
        return new Client(account, "Company", null, null, null);
    }
}
