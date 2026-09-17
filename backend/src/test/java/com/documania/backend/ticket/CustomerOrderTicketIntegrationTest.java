package com.documania.backend.ticket;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.CatalogService;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.offer.Offer;
import com.documania.backend.offer.OfferRepository;
import com.documania.backend.order.CustomerOrder;
import com.documania.backend.order.CustomerOrderRepository;
import com.documania.backend.order.CustomerOrderService;
import com.documania.backend.order.OrderStatus;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.subscription.Subscription;
import com.documania.backend.subscription.SubscriptionPeriod;
import com.documania.backend.subscription.SubscriptionPeriodRepository;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Vérifie l'intégration commande → ticket :
 *  - la création d'une commande en attente ouvre automatiquement un ticket
 *    ORDER_REQUIREMENTS lié à la commande ;
 *  - la confirmation de la commande clôture automatiquement ce ticket.
 */
class CustomerOrderTicketIntegrationTest {
    private CustomerOrderRepository orderRepository;
    private ClientRepository clientRepository;
    private OfferRepository offerRepository;
    private AuditService auditService;
    private UserAccountRepository userAccountRepository;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionPeriodRepository subscriptionPeriodRepository;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private NotificationService notificationService;
    private TicketRepository ticketRepository;
    private TicketMessageRepository ticketMessageRepository;
    private CustomerOrderService service;

    @BeforeEach void setUp() {
        orderRepository = mock(CustomerOrderRepository.class);
        clientRepository = mock(ClientRepository.class);
        offerRepository = mock(OfferRepository.class);
        auditService = mock(AuditService.class);
        userAccountRepository = mock(UserAccountRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        subscriptionPeriodRepository = mock(SubscriptionPeriodRepository.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        notificationService = mock(NotificationService.class);
        ticketRepository = mock(TicketRepository.class);
        ticketMessageRepository = mock(TicketMessageRepository.class);
        service = new CustomerOrderService(orderRepository, clientRepository, offerRepository, auditService,
            userAccountRepository, subscriptionRepository, subscriptionPeriodRepository,
            emailService, emailOutboxRepository, notificationService,
            ticketRepository, ticketMessageRepository);
    }

    private UserAccount clientAccount() {
        return new UserAccount("client@test.local", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
    }

    private Client client() {
        return new Client(clientAccount(), "Company", null, null, null);
    }

    private Offer offer() {
        CatalogService catalog = new CatalogService("Hosting", null, null);
        return new Offer(catalog, "Professional", null, new BigDecimal("199.00"), 12, 10,
            LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
    }

    private UserAccount admin() {
        return new UserAccount("admin@test.local", "hash",
            new Role(RoleName.ADMIN, "Admin"), "Admin", "User");
    }

    @Test void shouldAutoCreateOrderRequirementsTicketWhenOrderIsCreated() {
        UUID offerId = UUID.randomUUID();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client()));
        when(offerRepository.findByPublicIdAndArchived(offerId, false)).thenReturn(Optional.of(offer()));
        when(subscriptionRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(any(), any(), any()))
            .thenReturn(false);
        when(orderRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(any(), any(), any()))
            .thenReturn(false);
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));
        when(userAccountRepository.findAllByRole_NameOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(ticketRepository.existsByOrder_IdAndCategory(any(), eq(TicketCategory.ORDER_REQUIREMENTS)))
            .thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(call -> call.getArgument(0));

        CustomerOrder order = service.createForCurrentClient("client@test.local", offerId);

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket created = ticketCaptor.getValue();
        assertEquals(TicketCategory.ORDER_REQUIREMENTS, created.getCategory());
        assertEquals(TicketStatus.OPEN, created.getStatus());
        assertEquals("Company", created.getClientCompanyNameSnapshot());
        assertEquals(order, created.getOrder());
        assertEquals(order.getOrderNumber(), created.getSubject().substring(created.getSubject().lastIndexOf(" ") + 1));
        verify(ticketMessageRepository).save(any(TicketMessage.class));
        verify(auditService).record(eq(AuditAction.TICKET_CREATED), eq("TICKET"), any(), any(), any(), any(), any(), any());
    }

    @Test void shouldCloseRequirementsTicketWhenOrderIsConfirmed() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = new CustomerOrder(client(), offer(), "ORD-CONFIRM");
        Ticket linked = new Ticket(client(), order, null, TicketCategory.ORDER_REQUIREMENTS,
            com.documania.backend.ticket.TicketPriority.MEDIUM, "Exigences", "Description");
        when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
        when(subscriptionRepository.existsByCustomerOrder_Id(any())).thenReturn(false);
        when(userAccountRepository.findByEmailIgnoreCase("admin@test.local")).thenReturn(Optional.of(admin()));
        when(orderRepository.save(order)).thenReturn(order);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));
        when(subscriptionPeriodRepository.save(any(SubscriptionPeriod.class))).thenAnswer(call -> call.getArgument(0));
        when(emailService.buildOrderConfirmedMessage(eq("client@test.local"), any(), eq("Professional"), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));
        when(emailService.buildSubscriptionActivatedMessage(eq("client@test.local"), eq("Company"),
            eq("Professional"), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));
        when(ticketRepository.findByOrder_IdAndCategory(order.getId(), TicketCategory.ORDER_REQUIREMENTS))
            .thenReturn(Optional.of(linked));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(call -> call.getArgument(0));

        CustomerOrderService.ConfirmedOrder result = service.confirm("admin@test.local", orderId);

        assertEquals(OrderStatus.CONFIRMED, result.order().getStatus());
        assertEquals(TicketStatus.CLOSED, linked.getStatus());
        assertNotNull(linked.getClosedAt());
        verify(ticketRepository).save(linked);
        verify(ticketMessageRepository).save(any(TicketMessage.class));
    }

    @Test void shouldNotDuplicateRequirementsTicketWhenOneExists() {
        UUID offerId = UUID.randomUUID();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client()));
        when(offerRepository.findByPublicIdAndArchived(offerId, false)).thenReturn(Optional.of(offer()));
        when(subscriptionRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(any(), any(), any()))
            .thenReturn(false);
        when(orderRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(any(), any(), any()))
            .thenReturn(false);
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));
        when(userAccountRepository.findAllByRole_NameOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(ticketRepository.existsByOrder_IdAndCategory(any(), eq(TicketCategory.ORDER_REQUIREMENTS)))
            .thenReturn(true);

        CustomerOrder order = service.createForCurrentClient("client@test.local", offerId);

        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(ticketMessageRepository, never()).save(any(TicketMessage.class));
    }
}
