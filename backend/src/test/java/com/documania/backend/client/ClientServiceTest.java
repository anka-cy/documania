package com.documania.backend.client;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.client.dto.CreateClientRequest;
import com.documania.backend.client.dto.UpdateClientRequest;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.role.RoleRepository;
import com.documania.backend.order.CustomerOrderRepository;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.subscription.SubscriptionStatus;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClientServiceTest {

    private ClientRepository clientRepository;
    private UserAccountRepository userAccountRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private AuditService auditService;
    private CustomerOrderRepository customerOrderRepository;
    private SubscriptionRepository subscriptionRepository;
    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditService = mock(AuditService.class);
        customerOrderRepository = mock(CustomerOrderRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        clientService = new ClientService(
            clientRepository,
            userAccountRepository,
            roleRepository,
            passwordEncoder,
            auditService,
            customerOrderRepository,
            subscriptionRepository
        );
    }

    @Test
    void shouldCreateDisabledClientAccountOrganizationAndAudit() {
        CreateClientRequest request = new CreateClientRequest(
            " CONTACT@Company.test ",
            " Sara ",
            " Amrani ",
            " Example Company ",
            "   ",
            " Casablanca ",
            " Services "
        );
        Role clientRole = new Role(RoleName.CLIENT, "Client");
        when(userAccountRepository.existsByEmailIgnoreCase("contact@company.test"))
            .thenReturn(false);
        when(roleRepository.findByName(RoleName.CLIENT)).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode(anyString())).thenReturn("unusable-password-hash");
        when(userAccountRepository.save(any(UserAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(clientRepository.save(any(Client.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Client result = clientService.createClient(request);

        assertEquals("Example Company", result.getCompanyName());
        assertNull(result.getPhone());
        assertEquals("Casablanca", result.getAddress());
        assertEquals("Services", result.getSector());
        assertEquals("contact@company.test", result.getUserAccount().getEmail());
        assertEquals(RoleName.CLIENT, result.getUserAccount().getRole().getName());
        assertFalse(result.getUserAccount().isEnabled());
        assertFalse(result.getUserAccount().isEmailVerified());
        verify(auditService).record(
            AuditAction.CLIENT_CREATED,
            "CLIENT",
            null,
            null,
            "Example Company",
            Map.of(),
            Map.of(
                "email", "contact@company.test",
                "companyName", "Example Company",
                "enabled", false,
                "emailVerified", false
            ),
            null
        );
    }

    @Test
    void shouldRejectDuplicateEmailBeforeWritingAnything() {
        CreateClientRequest request = new CreateClientRequest(
            "contact@company.test", "Sara", "Amrani", "Company",
            null, null, null
        );
        when(userAccountRepository.existsByEmailIgnoreCase("contact@company.test"))
            .thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> clientService.createClient(request));

        verify(userAccountRepository, never()).save(any());
        verifyNoInteractions(clientRepository, roleRepository, passwordEncoder, auditService);
    }

    @Test
    void shouldListOnlyActiveClientsNewestFirst() {
        List<Client> clients = List.of(mock(Client.class), mock(Client.class));
        when(clientRepository.findAllByArchivedOrderByCreatedAtDesc(false))
            .thenReturn(clients);

        assertEquals(clients, clientService.listActiveClients());
        verify(clientRepository).findAllByArchivedOrderByCreatedAtDesc(false);
    }

    @Test
    void shouldListArchivedClientsNewestFirst() {
        List<Client> clients = List.of(mock(Client.class), mock(Client.class));
        when(clientRepository.findAllByArchivedOrderByCreatedAtDesc(true))
            .thenReturn(clients);

        assertEquals(clients, clientService.listArchivedClients());
        verify(clientRepository).findAllByArchivedOrderByCreatedAtDesc(true);
    }

    @Test
    void shouldFindOnlyActiveClientByPublicId() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Client client = mock(Client.class);
        when(clientRepository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.of(client));

        assertEquals(client, clientService.findActiveClient(publicId));
    }

    @Test
    void shouldHideMissingOrArchivedClientFromActiveEndpoint() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(clientRepository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            clientService.findActiveClient(publicId)
        );
    }

    @Test
    void shouldUpdateClientContactAndCompanyDetailsAndAudit() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Role role = new Role(RoleName.CLIENT, "Client");
        UserAccount account = new UserAccount(
            "contact@company.test", "hash", role, "Old", "Contact"
        );
        Client client = new Client(
            account, "Old Company", null, "Old Address", null
        );
        when(clientRepository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);
        when(userAccountRepository.save(account)).thenReturn(account);

        Client result = clientService.updateClient(
            publicId,
            new UpdateClientRequest(
                " Sara ", " Amrani ", " New Company ", " +212600000001 ",
                " Rabat ", " Technology "
            )
        );

        assertEquals("Sara", result.getUserAccount().getFirstName());
        assertEquals("Amrani", result.getUserAccount().getLastName());
        assertEquals("New Company", result.getCompanyName());
        assertEquals("+212600000001", result.getPhone());
        verify(userAccountRepository).save(account);
        verify(clientRepository).save(client);
        verify(auditService).record(
            org.mockito.ArgumentMatchers.eq(AuditAction.CLIENT_UPDATED),
            org.mockito.ArgumentMatchers.eq("CLIENT"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq("New Company"),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldArchiveDisableCancelPendingOrdersAndAudit() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserAccount account = new UserAccount(
            "contact@company.test", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        Client client = new Client(account, "Company", null, null, null);
        when(clientRepository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);
        when(customerOrderRepository.updateStatusForClientAndStatus(
            org.mockito.ArgumentMatchers.isNull(), any(), any(), any()
        )).thenReturn(2);

        Client result = clientService.archiveClient(publicId);

        assertTrue(result.isArchived());
        assertFalse(account.isEnabled());
        assertNotNull(result.getArchivedAt());
        verify(customerOrderRepository)
            .updateStatusForClientAndStatus(null, com.documania.backend.order.OrderStatus.PENDING, com.documania.backend.order.OrderStatus.CANCELLED, result.getArchivedAt());
        verify(auditService).record(
            org.mockito.ArgumentMatchers.eq(AuditAction.CLIENT_ARCHIVED),
            org.mockito.ArgumentMatchers.eq("CLIENT"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq("Company"),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRestoreButKeepUnverifiedAccountDisabled() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserAccount account = new UserAccount(
            "contact@company.test", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        account.disable();
        Client client = new Client(account, "Company", null, null, null);
        client.archive();
        when(clientRepository.findByPublicIdAndArchived(publicId, true))
            .thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        Client result = clientService.restoreClient(publicId);

        assertFalse(result.isArchived());
        assertFalse(account.isEnabled());
        verifyNoInteractions(customerOrderRepository);
    }

    @Test
    void shouldRestoreAndEnableVerifiedAccount() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserAccount account = new UserAccount(
            "contact@company.test", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        account.markEmailVerified();
        account.disable();
        Client client = new Client(account, "Company", null, null, null);
        client.archive();
        when(clientRepository.findByPublicIdAndArchived(publicId, true))
            .thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        Client result = clientService.restoreClient(publicId);

        assertFalse(result.isArchived());
        assertTrue(account.isEnabled());
    }

    @Test
    void shouldDeleteArchivedClientWithoutSubscriptionAndAudit() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserAccount account = new UserAccount(
            "contact@company.test", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        account.disable();
        Client client = new Client(account, "Company", null, null, null);
        client.archive();
        when(clientRepository.findByPublicIdAndArchived(publicId, true))
            .thenReturn(Optional.of(client));
        when(subscriptionRepository.existsByClient_IdAndStatus(null, SubscriptionStatus.ACTIVE)).thenReturn(false);

        clientService.deleteArchivedClient(publicId, "Company account permanently closed");

        verify(auditService).record(
            org.mockito.ArgumentMatchers.eq(AuditAction.CLIENT_DELETED),
            org.mockito.ArgumentMatchers.eq("CLIENT"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq("Company"),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.eq("Company account permanently closed")
        );
        verify(clientRepository).delete(client);
        verify(clientRepository).flush();
        verify(userAccountRepository).delete(account);
    }

    @Test
    void shouldRejectPermanentDeletionWhenSubscriptionExists() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserAccount account = new UserAccount(
            "contact@company.test", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        Client client = new Client(account, "Company", null, null, null);
        client.archive();
        when(clientRepository.findByPublicIdAndArchived(publicId, true))
            .thenReturn(Optional.of(client));
        when(subscriptionRepository.existsByClient_IdAndStatus(null, SubscriptionStatus.ACTIVE)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () ->
            clientService.deleteArchivedClient(publicId, "Company account permanently closed")
        );

        verify(userAccountRepository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldRejectPermanentDeletionOfActiveClient() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(clientRepository.findByPublicIdAndArchived(publicId, true))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            clientService.deleteArchivedClient(publicId, "Company account permanently closed")
        );

        verifyNoInteractions(subscriptionRepository, auditService);
    }
}
