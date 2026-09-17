package com.documania.backend.client;

import com.documania.backend.common.util.TextUtils;
import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.client.dto.CreateClientRequest;
import com.documania.backend.client.dto.UpdateClientRequest;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.subscription.SubscriptionStatus;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.role.RoleRepository;
import com.documania.backend.order.CustomerOrderRepository;
import com.documania.backend.order.OrderStatus;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CustomerOrderRepository customerOrderRepository;
    private final SubscriptionRepository subscriptionRepository;

    public ClientService(
        ClientRepository clientRepository,
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AuditService auditService,
        CustomerOrderRepository customerOrderRepository,
        SubscriptionRepository subscriptionRepository
    ) {
        this.clientRepository = clientRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.customerOrderRepository = customerOrderRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Client createClient(CreateClientRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Un compte utilise déjà cette adresse e-mail");
        }

        Role clientRole = roleRepository.findByName(RoleName.CLIENT)
            .orElseThrow(() -> new ResourceNotFoundException("Rôle CLIENT introuvable"));

        String unusablePasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        UserAccount account = new UserAccount(
            email,
            unusablePasswordHash,
            clientRole,
            request.firstName().trim(),
            request.lastName().trim()
        );
        account.disable();
        UserAccount savedAccount = userAccountRepository.save(account);

        Client client = new Client(
            savedAccount,
            request.companyName().trim(),
            TextUtils.optionalText(request.phone()),
            TextUtils.optionalText(request.address()),
            TextUtils.optionalText(request.sector())
        );
        Client savedClient = clientRepository.save(client);

        auditService.record(
            AuditAction.CLIENT_CREATED,
            "CLIENT",
            savedClient.getId(),
            savedClient.getPublicId(),
            savedClient.getCompanyName(),
            Map.of(),
            Map.of(
                "email", savedAccount.getEmail(),
                "companyName", savedClient.getCompanyName(),
                "enabled", savedAccount.isEnabled(),
                "emailVerified", savedAccount.isEmailVerified()
            ),
            null
        );

        return savedClient;
    }

    public List<Client> listActiveClients() {
        return clientRepository.findAllByArchivedOrderByCreatedAtDesc(false);
    }

    public List<Client> listArchivedClients() {
        return clientRepository.findAllByArchivedOrderByCreatedAtDesc(true);
    }

    public Client findActiveClient(UUID publicId) {
        return clientRepository.findByPublicIdAndArchived(publicId, false)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Client actif introuvable : " + publicId
            ));
    }

    public Client findClientById(UUID publicId) {
        return clientRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Client introuvable : " + publicId
            ));
    }

    @Transactional
    public Client updateClient(UUID publicId, UpdateClientRequest request) {
        Client client = findActiveClient(publicId);
        Map<String, Object> oldValues = clientSnapshot(client);

        client.getUserAccount().changeName(
            request.firstName().trim(),
            request.lastName().trim()
        );
        client.changeDetails(
            request.companyName().trim(),
            TextUtils.optionalText(request.phone()),
            TextUtils.optionalText(request.address()),
            TextUtils.optionalText(request.sector())
        );

        userAccountRepository.save(client.getUserAccount());
        Client savedClient = clientRepository.save(client);
        auditService.record(
            AuditAction.CLIENT_UPDATED,
            "CLIENT",
            savedClient.getId(),
            savedClient.getPublicId(),
            savedClient.getCompanyName(),
            oldValues,
            clientSnapshot(savedClient),
            null
        );
        return savedClient;
    }

    @Transactional
    public Client archiveClient(UUID publicId) {
        Client client = findActiveClient(publicId);
        boolean previouslyEnabled = client.getUserAccount().isEnabled();

        client.archive();
        client.getUserAccount().disable();
        int cancelledOrders = customerOrderRepository.updateStatusForClientAndStatus(
            client.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED, client.getArchivedAt()
        );

        userAccountRepository.save(client.getUserAccount());
        Client savedClient = clientRepository.save(client);
        auditService.record(
            AuditAction.CLIENT_ARCHIVED,
            "CLIENT",
            savedClient.getId(),
            savedClient.getPublicId(),
            savedClient.getCompanyName(),
            Map.of("archived", false, "enabled", previouslyEnabled),
            Map.of(
                "archived", true,
                "enabled", false,
                "cancelledPendingOrders", cancelledOrders
            ),
            null
        );
        return savedClient;
    }

    @Transactional
    public Client restoreClient(UUID publicId) {
        Client client = clientRepository.findByPublicIdAndArchived(publicId, true)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Client archivé introuvable : " + publicId
            ));

        client.restore();
        if (client.getUserAccount().isEmailVerified()) {
            client.getUserAccount().enable();
        } else {
            client.getUserAccount().disable();
        }

        userAccountRepository.save(client.getUserAccount());
        Client savedClient = clientRepository.save(client);
        auditService.record(
            AuditAction.CLIENT_RESTORED,
            "CLIENT",
            savedClient.getId(),
            savedClient.getPublicId(),
            savedClient.getCompanyName(),
            Map.of("archived", true, "enabled", false),
            Map.of(
                "archived", false,
                "enabled", savedClient.getUserAccount().isEnabled()
            ),
            null
        );
        return savedClient;
    }

    @Transactional
    public void deleteArchivedClient(UUID publicId, String reason) {
        Client client = clientRepository.findByPublicIdAndArchived(publicId, true)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Client archivé introuvable : " + publicId
            ));

        if (subscriptionRepository.existsByClient_IdAndStatus(client.getId(), SubscriptionStatus.ACTIVE)) {
            throw new BusinessRuleException(
                "Un client ayant un abonnement actif ne peut pas être supprimé définitivement"
            );
        }

        auditService.record(
            AuditAction.CLIENT_DELETED,
            "CLIENT",
            client.getId(),
            client.getPublicId(),
            client.getCompanyName(),
            clientSnapshot(client),
            Map.of(),
            reason.trim()
        );
        UserAccount clientAccount = client.getUserAccount();

        // Supprimer le Client avant le UserAccount : sinon Hibernate lève une
        // TransientPropertyValueException avant le cascade BD. FK orders/subscriptions en
        // ON DELETE SET NULL : lignes conservées en historique (client_id = NULL).
        clientRepository.delete(client);
        clientRepository.flush();
        userAccountRepository.delete(clientAccount);
    }

    private Map<String, Object> clientSnapshot(Client client) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("firstName", client.getUserAccount().getFirstName());
        values.put("lastName", client.getUserAccount().getLastName());
        values.put("companyName", client.getCompanyName());
        values.put("phone", client.getPhone());
        values.put("address", client.getAddress());
        values.put("sector", client.getSector());
        return values;
    }
}
