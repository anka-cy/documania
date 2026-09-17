package com.documania.backend.order;

import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.CatalogService;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.offer.Offer;
import com.documania.backend.offer.OfferRepository;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import com.documania.backend.subscription.Subscription;
import com.documania.backend.subscription.SubscriptionPeriod;
import com.documania.backend.subscription.SubscriptionPeriodRepository;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.subscription.SubscriptionStatus;
import com.documania.backend.ticket.Ticket;
import com.documania.backend.ticket.TicketCategory;
import com.documania.backend.ticket.TicketMessage;
import com.documania.backend.ticket.TicketMessageRepository;
import com.documania.backend.ticket.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomerOrderServiceTest {
    private CustomerOrderRepository orderRepository;
    private ClientRepository clientRepository;
    private OfferRepository offerRepository;
    private AuditService auditService;
    private CustomerOrderService service;
    private UserAccountRepository userAccountRepository;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionPeriodRepository subscriptionPeriodRepository;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private com.documania.backend.notification.NotificationService notificationService;
    private TicketRepository ticketRepository;
    private TicketMessageRepository ticketMessageRepository;

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
        notificationService = mock(com.documania.backend.notification.NotificationService.class);
        ticketRepository = mock(TicketRepository.class);
        ticketMessageRepository = mock(TicketMessageRepository.class);
        service = new CustomerOrderService(orderRepository, clientRepository, offerRepository, auditService,
            userAccountRepository, subscriptionRepository, subscriptionPeriodRepository,
            emailService, emailOutboxRepository, notificationService,
            ticketRepository, ticketMessageRepository);
    }

    @Test void shouldCreatePendingOrderUsingServerSnapshots() {
        UUID offerId = UUID.randomUUID();
        Client client = client();
        Offer offer = offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(offerRepository.findByPublicIdAndArchived(offerId, false)).thenReturn(Optional.of(offer));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));
        when(userAccountRepository.findAllByRole_NameInAndEnabledTrueAndEmailVerifiedTrue(
            List.of(com.documania.backend.role.RoleName.STAFF, com.documania.backend.role.RoleName.ADMIN)))
            .thenReturn(List.of(verifiedStaffAccount()));
        when(ticketRepository.existsByOrder_IdAndCategory(any(), eq(TicketCategory.ORDER_REQUIREMENTS))).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(call -> call.getArgument(0));

        CustomerOrder result = service.createForCurrentClient("client@test.local", offerId);

        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals("Company", result.getClientCompanyNameSnapshot());
        assertEquals("Professional", result.getOfferNameSnapshot());
        assertEquals("Hosting", result.getServiceNameSnapshot());
        assertEquals(new BigDecimal("199.00"), result.getPriceSnapshot());
        assertTrue(result.getOrderNumber().matches("ORD-[A-F0-9]{20}"));
        verify(auditService).record(any(), eq("ORDER"), any(), any(), any(), any(), any(), isNull());
        verify(emailOutboxRepository, never()).save(any());
        verify(notificationService).notifyAll(anyList(), eq(com.documania.backend.notification.NotificationType.NEW_ORDER_PENDING), any(), any(), any());
    }

    @Test void shouldRejectOfferOutsideCommercialDates() {
        UUID offerId = UUID.randomUUID();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived(any(), eq(false)))
            .thenReturn(Optional.of(client()));
        when(offerRepository.findByPublicIdAndArchived(offerId, false))
            .thenReturn(Optional.of(offer(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))));

        assertThrows(BusinessRuleException.class,
            () -> service.createForCurrentClient("client@test.local", offerId));
        verify(orderRepository, never()).save(any());
    }

    @Test void shouldRejectOrderWhenClientHasActiveSubscriptionForService() {
        UUID offerId = UUID.randomUUID();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client()));
        when(offerRepository.findByPublicIdAndArchived(offerId, false))
            .thenReturn(Optional.of(offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))));
        when(subscriptionRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(
            isNull(), eq(SubscriptionStatus.ACTIVE), isNull())).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
            () -> service.createForCurrentClient("client@test.local", offerId));
        assertTrue(ex.getMessage().contains("abonnement actif"));
        verify(orderRepository, never()).save(any());
    }

    @Test void shouldRejectOrderWhenClientHasPendingOrderForService() {
        UUID offerId = UUID.randomUUID();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client()));
        when(offerRepository.findByPublicIdAndArchived(offerId, false))
            .thenReturn(Optional.of(offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))));
        when(orderRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(
            isNull(), eq(OrderStatus.PENDING), isNull())).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
            () -> service.createForCurrentClient("client@test.local", offerId));
        assertTrue(ex.getMessage().contains("en attente"));
        verify(orderRepository, never()).save(any());
    }

    @Test void shouldAllowOwnerToCancelPendingOrder() {
        UUID orderId = UUID.randomUUID();
        Client client = mock(Client.class);
        when(client.getId()).thenReturn(42L);
        when(client.getCompanyName()).thenReturn("Company");
        CustomerOrder order = new CustomerOrder(client,
            offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), "ORD-TEST");
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(orderRepository.findByPublicIdAndClient_Id(orderId, 42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        CustomerOrder result = service.cancelForCurrentClient("client@test.local", orderId);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        verify(auditService).record(any(), eq("ORDER"), any(), any(), eq("ORD-TEST"), any(), any(), isNull());
    }

    @Test void shouldRejectCancellingOrderTwice() {
        UUID orderId = UUID.randomUUID();
        Client client = mock(Client.class);
        when(client.getId()).thenReturn(42L);
        when(client.getCompanyName()).thenReturn("Company");
        CustomerOrder order = new CustomerOrder(client,
            offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), "ORD-TEST");
        order.cancel(java.time.LocalDateTime.now());
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived(any(), eq(false)))
            .thenReturn(Optional.of(client));
        when(orderRepository.findByPublicIdAndClient_Id(orderId, 42L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class,
            () -> service.cancelForCurrentClient("client@test.local", orderId));
        verify(orderRepository, never()).save(any());
    }

    @Test void shouldHideAnotherClientsOrder() {
        UUID orderId = UUID.randomUUID();
        Client client = mock(Client.class);
        when(client.getId()).thenReturn(42L);
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived(any(), eq(false)))
            .thenReturn(Optional.of(client));
        when(orderRepository.findByPublicIdAndClient_Id(orderId, 42L)).thenReturn(Optional.empty());

        assertThrows(com.documania.backend.common.exception.ResourceNotFoundException.class,
            () -> service.cancelForCurrentClient("client@test.local", orderId));
    }

    @Test void shouldAllowAdministratorToRejectPendingOrderWithReason() {
        UUID orderId = UUID.randomUUID();
        Client client = client();
        CustomerOrder order = new CustomerOrder(client,
            offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), "ORD-REJECT");
        UserAccount admin = new UserAccount("admin@test.local", "hash",
            new Role(RoleName.ADMIN, "Admin"), "Admin", "User");
        when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
        when(userAccountRepository.findByEmailIgnoreCase("admin@test.local")).thenReturn(Optional.of(admin));
        when(orderRepository.save(order)).thenReturn(order);
        when(emailService.buildOrderRejectedMessage(eq("client@test.local"), any(), eq("Dossier incomplet")))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        CustomerOrder result = service.reject("admin@test.local", orderId, "  Dossier incomplet  ");

        assertEquals(OrderStatus.REJECTED, result.getStatus());
        assertEquals("Dossier incomplet", result.getRejectionReason());
        assertEquals(admin, result.getProcessedBy());
        assertNotNull(result.getProcessedAt());
        verify(emailOutboxRepository).save(any());
    }

    @Test void shouldRejectProcessingCancelledOrder() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = new CustomerOrder(client(),
            offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), "ORD-CANCELLED");
        order.cancel(java.time.LocalDateTime.now());
        when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class,
            () -> service.reject("admin@test.local", orderId, "Motif valide"));
        verifyNoInteractions(userAccountRepository);
    }

    @Test void shouldConfirmPendingOrderAndCreateInitialSubscriptionPeriod() {
        UUID orderId = UUID.randomUUID();
        Client client = client();
        CustomerOrder order = new CustomerOrder(client,
            offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), "ORD-CONFIRM");
        UserAccount admin = new UserAccount("admin@test.local", "hash",
            new Role(RoleName.ADMIN, "Admin"), "Admin", "User");
        when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
        when(subscriptionRepository.existsByCustomerOrder_Id(any())).thenReturn(false);
        when(userAccountRepository.findByEmailIgnoreCase("admin@test.local")).thenReturn(Optional.of(admin));
        when(orderRepository.save(order)).thenReturn(order);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));
        when(emailService.buildOrderConfirmedMessage(eq("client@test.local"), any(), eq("Professional"), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));
        when(emailService.buildSubscriptionActivatedMessage(eq("client@test.local"), eq("Company"),
            eq("Professional"), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        CustomerOrderService.ConfirmedOrder result = service.confirm("admin@test.local", orderId);

        assertEquals(OrderStatus.CONFIRMED, result.order().getStatus());
        assertEquals(com.documania.backend.subscription.SubscriptionStatus.ACTIVE, result.subscription().getStatus());
        assertEquals(LocalDate.now(), result.subscription().getStartDate());
        assertEquals(LocalDate.now().plusMonths(12).minusDays(1), result.subscription().getEndDate());
        org.mockito.ArgumentCaptor<SubscriptionPeriod> periodCaptor =
            org.mockito.ArgumentCaptor.forClass(SubscriptionPeriod.class);
        verify(subscriptionPeriodRepository).save(periodCaptor.capture());
        assertEquals(1, periodCaptor.getValue().getPeriodNumber());
        assertEquals(new BigDecimal("199.00"), periodCaptor.getValue().getPriceSnapshot());
        verify(emailOutboxRepository, times(2)).save(any());
    }

    @Test void shouldRejectConfirmingAnAlreadyProcessedOrder() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = new CustomerOrder(client(),
            offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), "ORD-OLD");
        order.cancel(java.time.LocalDateTime.now());
        when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> service.confirm("admin@test.local", orderId));
        verifyNoInteractions(subscriptionRepository, subscriptionPeriodRepository);
    }

    @Test void shouldListOrdersPaginatedForStaffPortal() {
        CustomerOrder order = new CustomerOrder(client(),
            offer(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), "ORD-PAGE");
        when(orderRepository.search(isNull(), isNull(), eq(
            org.springframework.data.domain.PageRequest.of(0, 20,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(order),
                org.springframework.data.domain.PageRequest.of(0, 20), 1));

        com.documania.backend.order.dto.OrderPageResponse response = service.listAll(null, null, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals("ORD-PAGE", response.items().get(0).orderNumber());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1, response.totalElements());
    }

    private Client client() {
        UserAccount account = new UserAccount("client@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        return new Client(account, "Company", null, null, null);
    }

    private Offer offer(LocalDate start, LocalDate end) {
        CatalogService catalog = new CatalogService("Hosting", null, null);
        return new Offer(catalog, "Professional", null, new BigDecimal("199.00"), 12, 10, start, end);
    }

    private UserAccount verifiedStaffAccount() {
        UserAccount account = new UserAccount("staff@test.local", "hash",
            new Role(RoleName.STAFF, "Personnel"), "Staff", "User");
        account.markEmailVerified();
        return account;
    }
}
