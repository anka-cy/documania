package com.documania.backend.ticket;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.client.Client;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.ticket.dto.AddTicketMessageRequest;
import com.documania.backend.ticket.dto.CreateTicketTaskRequest;
import com.documania.backend.ticket.dto.TicketDetailResponse;
import com.documania.backend.ticket.dto.UpdateTicketTaskRequest;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StaffTicketServiceTest {
    private TicketRepository ticketRepository;
    private TicketMessageRepository ticketMessageRepository;
    private TicketTaskRepository ticketTaskRepository;
    private UserAccountRepository userAccountRepository;
    private AuditService auditService;
    private EmailService emailService;
    private EmailOutboxRepository emailOutboxRepository;
    private NotificationService notificationService;
    private TicketReadMarkRepository readMarkRepository;
    private TicketAttachmentRepository attachmentRepository;
    private TicketAttachmentService attachmentService;
    private StaffTicketService service;

    @BeforeEach void setUp() {
        ticketRepository = mock(TicketRepository.class);
        ticketMessageRepository = mock(TicketMessageRepository.class);
        ticketTaskRepository = mock(TicketTaskRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        auditService = mock(AuditService.class);
        emailService = mock(EmailService.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        notificationService = mock(NotificationService.class);
        readMarkRepository = mock(TicketReadMarkRepository.class);
        attachmentRepository = mock(TicketAttachmentRepository.class);
        attachmentService = mock(TicketAttachmentService.class);
        service = new StaffTicketService(ticketRepository, ticketMessageRepository, ticketTaskRepository,
            userAccountRepository, auditService, emailService, emailOutboxRepository, notificationService,
            readMarkRepository, attachmentRepository, attachmentService);
    }

    private UserAccount staff(String email) {
        UserAccount staff = new UserAccount(email, "hash", new Role(RoleName.STAFF, "Staff"), "Staff", "User");
        staff.markEmailVerified();
        return staff;
    }

    private Client client() {
        UserAccount account = new UserAccount("client@test.local", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        return new Client(account, "Company", null, null, null);
    }

    private Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket(client(), null, null, TicketCategory.GENERAL,
            TicketPriority.MEDIUM, "Sujet", "Description");
        org.springframework.test.util.ReflectionTestUtils.setField(ticket, "publicId", UUID.randomUUID());
        if (status != TicketStatus.OPEN) {
            ticket.changeStatus(status, java.time.LocalDateTime.now());
        }
        return ticket;
    }

    @Test void shouldPostPublicMessageForClient() {
        UserAccount actor = staff("staff@test.local");
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(userAccountRepository.findByEmailIgnoreCase("staff@test.local")).thenReturn(Optional.of(actor));
        when(ticketRepository.findByPublicId(any())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(List.of());
        when(emailService.buildTicketReplyMessage(any(), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        TicketDetailResponse detail = service.addStaffMessage("staff@test.local", UUID.randomUUID(),
            new AddTicketMessageRequest("Message public pour le client", List.of()));

        // Tous les messages du staff sont publics (pas de note interne).
        verify(ticketMessageRepository).save(argThat(msg -> !msg.isInternal()));
        assertNotNull(detail);
    }

    @Test void shouldAddTaskAndComputeProgress() {
        UserAccount actor = staff("staff@test.local");
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(userAccountRepository.findByEmailIgnoreCase("staff@test.local")).thenReturn(Optional.of(actor));
        when(ticketRepository.findByPublicId(any())).thenReturn(Optional.of(ticket));
        when(ticketTaskRepository.countByTicket_Id(any())).thenReturn(2L);
        when(ticketTaskRepository.save(any(TicketTask.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(
            List.of(task(ticket, "Tâche existante", TicketTaskStatus.COMPLETED),
                task(ticket, "Autre tâche", TicketTaskStatus.PENDING)));

        TicketDetailResponse detail = service.addTask("staff@test.local", UUID.randomUUID(),
            new CreateTicketTaskRequest("Configurer l'environnement", "Installer les dépendances"));

        org.mockito.ArgumentCaptor<TicketTask> captor =
            org.mockito.ArgumentCaptor.forClass(TicketTask.class);
        verify(ticketTaskRepository).save(captor.capture());
        assertEquals("Configurer l'environnement", captor.getValue().getTitle());
        assertEquals(2, captor.getValue().getPosition());
        assertEquals(TicketTaskStatus.PENDING, captor.getValue().getStatus());
    }

    @Test void shouldCycleTaskStatusAndComputePercentage() {
        UserAccount actor = staff("staff@test.local");
        Ticket ticket = ticket(TicketStatus.OPEN);
        TicketTask task = new TicketTask(ticket, "Tâche unique", null, 0);
        when(userAccountRepository.findByEmailIgnoreCase("staff@test.local")).thenReturn(Optional.of(actor));
        when(ticketRepository.findByPublicId(any())).thenReturn(Optional.of(ticket));
        when(ticketTaskRepository.findByPublicIdAndTicket_Id(any(), any())).thenReturn(Optional.of(task));
        when(ticketTaskRepository.save(any(TicketTask.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(List.of(task));

        TicketDetailResponse detail = service.updateTask("staff@test.local", UUID.randomUUID(),
            UUID.randomUUID(), new UpdateTicketTaskRequest(null, null, TicketTaskStatus.COMPLETED));

        assertEquals(100, detail.progressPercent());
        assertEquals(TicketTaskStatus.COMPLETED, task.getStatus());
    }

    @Test void shouldCloseTicketWithReasonAndNotifyClient() {
        UserAccount actor = staff("staff@test.local");
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(userAccountRepository.findByEmailIgnoreCase("staff@test.local")).thenReturn(Optional.of(actor));
        when(ticketRepository.findByPublicId(any())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(List.of());
        when(emailService.buildTicketClosedMessage(any(), any(), any()))
            .thenReturn(new EmailService.EmailMessage("client@test.local", "s", "b"));

        service.closeTicket("staff@test.local", UUID.randomUUID(), "Demande traitée");

        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
        verify(notificationService).notify(any(), eq(com.documania.backend.notification.NotificationType.TICKET_CLOSED),
            any(), any(), any());
    }

    @Test void shouldRejectTaskOnClosedTicket() {
        UserAccount actor = staff("staff@test.local");
        Ticket ticket = ticket(TicketStatus.CLOSED);
        when(userAccountRepository.findByEmailIgnoreCase("staff@test.local")).thenReturn(Optional.of(actor));
        when(ticketRepository.findByPublicId(any())).thenReturn(Optional.of(ticket));

        assertThrows(BusinessRuleException.class, () -> service.addTask("staff@test.local",
            UUID.randomUUID(), new CreateTicketTaskRequest("Tâche", null)));
    }

    @Test void shouldDeleteTicketAndPurgeChildrenWithAudit() {
        UserAccount actor = staff("staff@test.local");
        Ticket ticket = ticket(TicketStatus.CLOSED);
        TicketTask task = task(ticket, "Tâche", TicketTaskStatus.COMPLETED);
        TicketMessage message = new TicketMessage(ticket, actor, "Message", false);
        when(userAccountRepository.findByEmailIgnoreCase("staff@test.local")).thenReturn(Optional.of(actor));
        when(ticketRepository.findByPublicId(any())).thenReturn(Optional.of(ticket));
        when(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(any())).thenReturn(List.of(task));
        when(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(any())).thenReturn(List.of(message));

        service.deleteTicket("staff@test.local", UUID.randomUUID(), "Doublon ouvert par erreur");

        verify(ticketTaskRepository).deleteAll(List.of(task));
        verify(ticketMessageRepository).deleteAll(List.of(message));
        verify(ticketRepository).delete(ticket);
        verify(auditService).record(eq(AuditAction.TICKET_DELETED), eq("TICKET"), any(), any(), any(), any(), any(), eq("Doublon ouvert par erreur"));
    }

    private TicketTask task(Ticket ticket, String title, TicketTaskStatus status) {
        TicketTask task = new TicketTask(ticket, title, null, 0);
        if (status != TicketTaskStatus.PENDING) {
            task.changeStatus(status, java.time.LocalDateTime.now());
        }
        return task;
    }
}