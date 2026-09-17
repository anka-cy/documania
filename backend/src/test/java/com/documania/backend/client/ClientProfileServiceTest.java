package com.documania.backend.client;

import com.documania.backend.account.AccountPasswordService;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientProfileServiceTest {

    private ClientRepository clientRepository;
    private AccountPasswordService accountPasswordService;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private com.documania.backend.notification.NotificationService notificationService;
    private ClientProfileService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        accountPasswordService = mock(AccountPasswordService.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        notificationService = mock(com.documania.backend.notification.NotificationService.class);
        service = new ClientProfileService(
            clientRepository, accountPasswordService, emailService, emailOutboxRepository, notificationService
        );
    }

    private Client activeClient() {
        UserAccount account = new UserAccount(
            "client@test.local", "current-hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        account.markEmailVerified();
        return new Client(account, "Sara Corp", null, null, null);
    }

    @Test
    void shouldReturnProfileForAuthenticatedEmail() {
        Client client = activeClient();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));

        Client result = service.getProfile("client@test.local");

        assertEquals(client, result);
    }

    @Test
    void shouldRejectProfileWhenClientIsArchivedOrMissing() {
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getProfile("client@test.local"));
    }

    @Test
    void shouldDelegatePasswordChangeAndQueueEmail() {
        Client client = activeClient();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(accountPasswordService.changePassword(
            client.getUserAccount(), "current-password", "A-new-secure-password", "A-new-secure-password"))
            .thenReturn(client.getUserAccount());
        when(emailService.buildPasswordChangedMessage("client@test.local"))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        service.changePassword(
            "client@test.local", "current-password", "A-new-secure-password", "A-new-secure-password"
        );

        org.mockito.Mockito.verify(emailOutboxRepository).save(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(notificationService).notify(
            org.mockito.ArgumentMatchers.eq(client.getUserAccount()),
            org.mockito.ArgumentMatchers.eq(com.documania.backend.notification.NotificationType.PASSWORD_CHANGED),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("#/client/profile"));
    }
}
