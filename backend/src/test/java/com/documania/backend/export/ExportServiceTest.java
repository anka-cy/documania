package com.documania.backend.export;

import com.documania.backend.catalog.CatalogService;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.offer.Offer;
import com.documania.backend.order.CustomerOrder;
import com.documania.backend.order.CustomerOrderRepository;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.subscription.Subscription;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportServiceTest {

    private ClientRepository clientRepository;
    private CustomerOrderRepository customerOrderRepository;
    private SubscriptionRepository subscriptionRepository;
    private ExportService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        customerOrderRepository = mock(CustomerOrderRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        service = new ExportService(clientRepository, customerOrderRepository, subscriptionRepository);
    }

    @Test
    void shouldGenerateXlsxForClients() {
        Client client = clientFixture();
        when(clientRepository.findAllByArchivedOrderByCreatedAtDesc(false)).thenReturn(List.of(client));
        when(clientRepository.findAllByArchivedOrderByCreatedAtDesc(true)).thenReturn(List.of());

        byte[] xlsx = service.exportClients();
        assertTrue(isXlsx(xlsx));
    }

    @Test
    void shouldGenerateXlsxForOrders() {
        when(customerOrderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(orderFixture()));

        assertTrue(isXlsx(service.exportOrders()));
    }

    @Test
    void shouldGenerateXlsxForSubscriptions() {
        when(subscriptionRepository.findAllByOrderByCreatedAtDesc())
            .thenReturn(List.of(subscriptionFixture()));

        assertTrue(isXlsx(service.exportSubscriptions()));
    }

    @Test
    void shouldProduceNonEmptyFiles() {
        when(clientRepository.findAllByArchivedOrderByCreatedAtDesc(false)).thenReturn(List.of(clientFixture()));
        when(clientRepository.findAllByArchivedOrderByCreatedAtDesc(true)).thenReturn(List.of());

        assertTrue(service.exportClients().length > 0);
    }

    private boolean isXlsx(byte[] content) {
        return content.length > 2 && content[0] == 'P' && content[1] == 'K';
    }

    private Client clientFixture() {
        UserAccount account = new UserAccount(
            "client@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        return new Client(account, "Example Company", "+212600000000", "Rabat", "Tech");
    }

    private CustomerOrder orderFixture() {
        Client client = clientFixture();
        CatalogService service = new CatalogService("GED", "Gestion de documents", "GED");
        Offer offer = new Offer(service, "Annuel", "Offre annuelle", new BigDecimal("1200.00"), 12, 5, LocalDate.now(), LocalDate.now().plusYears(1));
        return new CustomerOrder(client, offer, "CMD-001");
    }

    private Subscription subscriptionFixture() {
        Client client = clientFixture();
        CatalogService service = new CatalogService("GED", "Gestion de documents", "GED");
        Offer offer = new Offer(service, "Annuel", "Offre annuelle", new BigDecimal("1200.00"), 12, 5, LocalDate.now(), LocalDate.now().plusYears(1));
        CustomerOrder order = new CustomerOrder(client, offer, "CMD-001");
        return new Subscription(client, offer, order, LocalDate.now(), LocalDate.now().plusMonths(12));
    }
}