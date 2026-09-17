package com.documania.backend.ticket;

import com.documania.backend.audit.AuditService;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.subscription.Subscription;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.ticket.dto.AddTicketMessageRequest;
import com.documania.backend.ticket.dto.CreateTicketRequest;
import com.documania.backend.ticket.dto.TicketDetailResponse;
import com.documania.backend.ticket.dto.TicketMessageResponse;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerTicketServiceTest {
    private TicketRepository ticketRepository;
    private TicketMessageRepository ticketMessageRepository;
    private TicketTaskRepository ticketTaskRepository;
    private ClientRepository clientRepository;
    private SubscriptionRepository subscriptionRepository;
    private UserAccountRepository userAccountRepository;
    private AuditService auditService;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private NotificationService notificationService;
    private TicketReadMarkRepository readMarkRepository;
    private TicketAttachmentRepository attachmentRepository;
    private TicketAttachmentService attachmentService;
    private CustomerTicketService service;

    @BeforeEach void setUp() {
        ticketRepository = mock(TicketRepository.class);
        ticketMessageRepository = mock(TicketMessageRepository.class);
        ticketTaskRepository = mock(TicketTaskRepository.class);
        clientRepository = mock(ClientRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        auditService = mock(AuditService.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        notificationService = mock(NotificationService.class);
        readMarkRepository = mock(TicketReadMarkRepository.class);
        attachmentRepository = mock(TicketAttachmentRepository.class);
        attachmentService = mock(TicketAttachmentService.class);
        service = new CustomerTicketService(ticketRepository, ticketMessageRepository, ticketTaskRepository,
            clientRepository, subscriptionRepository, userAccountRepository, auditService,
            emailService, emailOutboxRepository, notificationService, readMarkRepository,
            attachmentRepository, attachmentService);
    }

    private UserAccount clientAccount() {
        return new UserAccount("client@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
    }

    private Client client() {
        return new Client(clientAccount(), "Company", null, null, null);
    }

    private Subscription subscription(Client owner) {
        com.documania.backend.catalog.CatalogService catalog =
            new com.documania.backend.catalog.CatalogService("Hosting", null, null);
        com.documania.backend.offer.Offer offer = new com.documania.backend.offer.Offer(
            catalog, "Professional", null, java.math.BigDecimal.valueOf(199), 12, 10,
            java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now().plusDays(30));
        return new Subscription(owner, offer, null,
            java.time.LocalDate.now(), java.time.LocalDate.now().plusMonths(1));
    }

    /** Simule la persistance : @PrePersist pose le publicId. */
    private Ticket savedTicket(Ticket ticket) {
        org.springframework.test.util.ReflectionTestUtils.setField(ticket, "publicId", UUID.randomUUID());
        return ticket;
    }

    private Client clientWithId(Long id) {
        Client c = client();
        org.springframework.test.util.ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private Ticket ticket(Client client, TicketStatus status) {
        Ticket ticket = new Ticket(client, null, null, TicketCategory.GENERAL, TicketPriority.MEDIUM,
            "Sujet", "Description du ticket");
        org.springframework.test.util.ReflectionTestUtils.setField(ticket, "publicId", UUID.randomUUID());
        if (status != TicketStatus.OPEN) {
            ticket.changeStatus(status, java.time.LocalDateTime.now());
        }
        return ticket;
    }

    @Test void shouldListOnlyOwnTickets() {
        Client client = client();
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(ticketRepository.findAllByClient_IdOrderByCreatedAtDesc(any()))
            .thenReturn(List.of(ticket(client, TicketStatus.OPEN)));

        var result = service.listForCurrentClient("client@test.local");

        assertEquals(1, result.size());
        assertEquals(TicketCategory.GENERAL, result.get(0).category());
        verify(ticketRepository).findAllByClient_IdOrderByCreatedAtDesc(client.getId());
    }

    @Test void shouldHideInternalNotesFromClientDetail() {
        Client client = client();
        Ticket ticket = ticket(client, TicketStatus.OPEN);
        UserAccount staff = new UserAccount("staff@test.local", "hash",
            new Role(RoleName.STAFF, "Staff"), "Staff", "User");
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(ticketRepository.findForReadByPublicIdAndClient_Id(any(), any())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of(
            new TicketMessage(ticket, staff, "Message public", false),
            new TicketMessage(ticket, staff, "Note interne", true)));
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(List.of());

        TicketDetailResponse detail = service.detailForCurrentClient("client@test.local", UUID.randomUUID());

        assertEquals(1, detail.messages().size());
        assertEquals("Message public", detail.messages().get(0).message());
        assertTrue(detail.messages().stream().noneMatch(TicketMessageResponse::internal));
    }

    @Test void shouldCreateIncidentTicketLinkedToOwnSubscription() {
        Client client = clientWithId(10L);
        Subscription subscription = subscription(client);
        org.springframework.test.util.ReflectionTestUtils.setField(subscription, "publicId", UUID.randomUUID());
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(subscriptionRepository.findByPublicId(any())).thenReturn(Optional.of(subscription));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> savedTicket(call.getArgument(0)));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(List.of());
        when(emailService.buildTicketOpenedMessage(any(), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));
        when(userAccountRepository.findAllByRole_NameOrderByCreatedAtDesc(any())).thenReturn(List.of());

        TicketDetailResponse detail = service.createForCurrentClient("client@test.local",
            new CreateTicketRequest(subscription.getPublicId(), TicketCategory.SERVICE_DOWN,
                TicketPriority.HIGH, "Panne", "Le module ne répond plus."));

        assertEquals(TicketCategory.SERVICE_DOWN, detail.ticket().category());
        assertEquals(TicketPriority.HIGH, detail.ticket().priority());
        assertEquals(TicketStatus.OPEN, detail.ticket().status());
    }

    @Test void shouldRejectIncidentOnAnotherClientsSubscription() {
        Client other = clientWithId(20L);
        Subscription subscription = subscription(other);
        org.springframework.test.util.ReflectionTestUtils.setField(subscription, "publicId", UUID.randomUUID());
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(clientWithId(10L)));
        when(subscriptionRepository.findByPublicId(any())).thenReturn(Optional.of(subscription));

        assertThrows(BusinessRuleException.class, () -> service.createForCurrentClient("client@test.local",
            new CreateTicketRequest(subscription.getPublicId(), TicketCategory.SERVICE_DOWN,
                TicketPriority.HIGH, "Panne", "Le module ne répond plus.")));
    }

    @Test void shouldPostClientMessageAndMoveOpenTicketToInProgress() {
        Client client = client();
        Ticket ticket = ticket(client, TicketStatus.OPEN);
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(ticketRepository.findByPublicIdAndClient_Id(any(), any())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(userAccountRepository.findAllByRole_NameOrderByCreatedAtDesc(any())).thenReturn(List.of());

        service.addClientMessage("client@test.local", UUID.randomUUID(),
            new AddTicketMessageRequest("Voici les précisions demandées.", List.of()));

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test void shouldRejectReplyOnClosedTicket() {
        Client client = client();
        Ticket ticket = ticket(client, TicketStatus.CLOSED);
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(ticketRepository.findByPublicIdAndClient_Id(any(), any())).thenReturn(Optional.of(ticket));

        assertThrows(BusinessRuleException.class, () -> service.addClientMessage("client@test.local",
            UUID.randomUUID(), new AddTicketMessageRequest("Réponse après clôture", List.of())));
    }

    @Test void shouldHideAnotherClientsTicket() {
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client()));
        when(ticketRepository.findForReadByPublicIdAndClient_Id(any(), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.detailForCurrentClient("client@test.local", UUID.randomUUID()));
    }

    @Test void shouldComputeProgressPercent() {
        var completed = new com.documania.backend.ticket.dto.TicketTaskResponse(
            UUID.randomUUID(), "t1", null, TicketTaskStatus.COMPLETED, 0, null, null, null);
        var pending = new com.documania.backend.ticket.dto.TicketTaskResponse(
            UUID.randomUUID(), "t2", null, TicketTaskStatus.PENDING, 1, null, null, null);
        var inProgress = new com.documania.backend.ticket.dto.TicketTaskResponse(
            UUID.randomUUID(), "t3", null, TicketTaskStatus.IN_PROGRESS, 2, null, null, null);

        assertEquals(33, CustomerTicketService.computeProgressPercent(List.of(completed, pending, inProgress)));
        assertEquals(0, CustomerTicketService.computeProgressPercent(List.of()));
    }
}
