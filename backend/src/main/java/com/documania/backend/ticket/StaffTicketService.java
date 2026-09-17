package com.documania.backend.ticket;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.notification.NotificationType;
import com.documania.backend.ticket.dto.AddTicketMessageRequest;
import com.documania.backend.ticket.dto.CreateTicketTaskRequest;
import com.documania.backend.ticket.dto.TicketDetailResponse;
import com.documania.backend.ticket.dto.TicketMessageResponse;
import com.documania.backend.ticket.dto.TicketResponse;
import com.documania.backend.ticket.dto.TicketTaskResponse;
import com.documania.backend.ticket.dto.UpdateTicketRequest;
import com.documania.backend.ticket.dto.UpdateTicketTaskRequest;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StaffTicketService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketTaskRepository ticketTaskRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationService notificationService;
    private final TicketReadMarkRepository readMarkRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketAttachmentService attachmentService;

    public StaffTicketService(TicketRepository ticketRepository,
                              TicketMessageRepository ticketMessageRepository,
                              TicketTaskRepository ticketTaskRepository,
                              UserAccountRepository userAccountRepository,
                              AuditService auditService,
                              EmailService emailService,
                              EmailOutboxRepository emailOutboxRepository,
                              NotificationService notificationService,
                              TicketReadMarkRepository readMarkRepository,
                              TicketAttachmentRepository attachmentRepository,
                              TicketAttachmentService attachmentService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketTaskRepository = ticketTaskRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationService = notificationService;
        this.readMarkRepository = readMarkRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentService = attachmentService;
    }

    public List<TicketResponse> listAll(String email) {
        UserAccount actor = user(email);
        List<Ticket> tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, Integer> unreadByTicket = unreadCounts(actor.getId(),
            tickets.stream().map(Ticket::getId).toList());
        return tickets.stream()
            .map(ticket -> TicketMapper.toResponse(ticket,
                unreadByTicket.getOrDefault(ticket.getId(), 0)))
            .toList();
    }

    public TicketDetailResponse detail(String email, UUID ticketPublicId) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findForReadByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        return buildDetail(ticket, actor);
    }

    private TicketDetailResponse buildDetail(Ticket ticket, UserAccount actor) {
        List<TicketMessage> messages = ticketMessageRepository
            .findAllByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        List<TicketMessageResponse> messageResponses = messages.stream()
            .map(message -> TicketMapper.toMessageResponse(message,
                attachmentResponsesForMessage(message)))
            .toList();
        List<TicketTaskResponse> tasks = ticketTaskRepository
            .findAllByTicket_IdOrderByPositionAscCreatedAtAsc(ticket.getId()).stream()
            .map(TicketMapper::toTaskResponse).toList();
        int progressPercent = CustomerTicketService.computeProgressPercent(tasks);
        int unread = 0;
        if (ticket.getId() != null) {
            unread = unreadCounts(actor.getId(), List.of(ticket.getId()))
                .getOrDefault(ticket.getId(), 0);
        }
        return new TicketDetailResponse(TicketMapper.toResponse(ticket, unread), messageResponses, tasks, progressPercent);
    }

    private List<com.documania.backend.ticket.dto.TicketAttachmentResponse> attachmentResponsesForMessage(TicketMessage message) {
        if (message.getId() == null) return List.of();
        return attachmentRepository.findAllByMessage_IdOrderByCreatedAtAsc(message.getId()).stream()
            .map(TicketMapper::toAttachmentResponse).toList();
    }

    /** Marque la discussion du ticket comme lue pour le staff connecté. */
    @Transactional
    public TicketDetailResponse markRead(String email, UUID ticketPublicId) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findForReadByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        Long userId = actor.getId();
        TicketReadMark mark = readMarkRepository.findByTicket_IdAndUser_Id(ticket.getId(), userId)
            .orElseGet(() -> new TicketReadMark(ticket, actor, LocalDateTime.now()));
        mark.markRead(LocalDateTime.now());
        readMarkRepository.save(mark);
        return buildDetail(ticket, actor);
    }

    /** Nombre de messages non lus par ticket pour un utilisateur (une requête). */
    private Map<Long, Integer> unreadCounts(Long userId, List<Long> ticketIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (ticketIds.isEmpty()) return result;
        List<Object[]> rows = readMarkRepository.countUnreadForTickets(userId, ticketIds);
        if (rows == null) return result;
        for (Object[] row : rows) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return result;
    }

    @Transactional
    public TicketDetailResponse addStaffMessage(String email, UUID ticketPublicId, AddTicketMessageRequest request) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Ce ticket est clôturé");
        }
        List<UUID> attachmentIds = request.attachmentIds() == null ? List.of() : request.attachmentIds();
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isBlank() && attachmentIds.isEmpty()) {
            throw new BusinessRuleException("Un message ou une pièce jointe est requis");
        }
        TicketMessage savedMessage = ticketMessageRepository.save(new TicketMessage(ticket, actor, message, false));

        // Lie les pièces jointes pré-uploadées au message.
        attachmentService.linkToMessage(savedMessage, attachmentIds);

        if (ticket.getClient() != null) {
            String ticketReference = CustomerTicketService.ticketReference(ticket);
            EmailService.EmailMessage replyMessage =
                emailService.buildTicketReplyMessage(ticket.getClient().getUserAccount().getEmail(),
                    ticket.getSubject(), ticketReference);
            emailOutboxRepository.save(new EmailOutbox(
                replyMessage.recipient(), replyMessage.subject(), replyMessage.body()));
            notificationService.notify(ticket.getClient().getUserAccount(), NotificationType.TICKET_MESSAGE,
                "Réponse sur votre demande",
                "Notre équipe a répondu à « " + ticket.getSubject() + " ».",
                "#/client/tickets/" + ticket.getPublicId());
        }
        return buildDetail(ticketRepository.findById(ticket.getId()).orElse(ticket), actor);
    }

    @Transactional
    public TicketDetailResponse updateTicket(String email, UUID ticketPublicId, UpdateTicketRequest request) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));

        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            ticket.changePriority(request.priority());
        }

        Ticket saved = ticketRepository.save(ticket);
        return buildDetail(saved, actor);
    }

    /**
     * Clôture manuelle d'un ticket par le staff (motif obligatoire) ;
     * notifie le client par e-mail et notification in-app.
     */
    @Transactional
    public TicketDetailResponse closeTicket(String email, UUID ticketPublicId, String reason) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Ce ticket est déjà clôturé");
        }
        String normalizedReason = reason.trim();
        if (normalizedReason.length() < 5) {
            throw new BusinessRuleException("Le motif de clôture doit contenir au moins 5 caractères");
        }
        LocalDateTime now = LocalDateTime.now();
        ticket.changeStatus(TicketStatus.CLOSED, now);
        Ticket saved = ticketRepository.save(ticket);

        auditService.record(AuditAction.TICKET_CLOSED, "TICKET", saved.getId(), saved.getPublicId(),
            saved.getSubject(), Map.of("status", "OPEN/IN_PROGRESS"),
            Map.of("status", TicketStatus.CLOSED.name(), "closedAt", now, "closedBy", actor.getEmail()),
            normalizedReason);

        if (saved.getClient() != null) {
            String ticketReference = CustomerTicketService.ticketReference(saved);
            EmailService.EmailMessage closedMessage =
                emailService.buildTicketClosedMessage(saved.getClient().getUserAccount().getEmail(),
                    saved.getSubject(), ticketReference);
            emailOutboxRepository.save(new EmailOutbox(
                closedMessage.recipient(), closedMessage.subject(), closedMessage.body()));
            notificationService.notify(saved.getClient().getUserAccount(), NotificationType.TICKET_CLOSED,
                "Votre demande est clôturée",
                "Votre demande « " + saved.getSubject() + " » a été clôturée.",
                "#/client/tickets/" + saved.getPublicId());
        }
        return buildDetail(saved, actor);
    }

    @Transactional
    public TicketDetailResponse addTask(String email, UUID ticketPublicId, CreateTicketTaskRequest request) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Ce ticket est clôturé : impossible d'ajouter des tâches");
        }
        int position = (int) ticketTaskRepository.countByTicket_Id(ticket.getId());
        TicketTask task = ticketTaskRepository.save(
            new TicketTask(ticket, request.title().trim(), request.description(), position));

        auditService.record(AuditAction.TICKET_TASK_CREATED, "TICKET_TASK", task.getId(), task.getPublicId(),
            task.getTitle(), Map.of(), Map.of("ticketId", ticket.getPublicId(),
                "position", position), null);
        return buildDetail(ticketRepository.findById(ticket.getId()).orElse(ticket), actor);
    }

    @Transactional
    public TicketDetailResponse updateTask(String email, UUID ticketPublicId, UUID taskPublicId,
                                           UpdateTicketTaskRequest request) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        TicketTask task = ticketTaskRepository.findByPublicIdAndTicket_Id(taskPublicId, ticket.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable : " + taskPublicId));

        if (request.title() != null) {
            task.update(request.title().trim(), request.description());
        }
        if (request.status() != null && request.status() != task.getStatus()) {
            TicketTaskStatus oldStatus = task.getStatus();
            task.changeStatus(request.status(), LocalDateTime.now());
            auditService.record(AuditAction.TICKET_TASK_STATUS_CHANGED, "TICKET_TASK", task.getId(),
                task.getPublicId(), task.getTitle(), Map.of("status", oldStatus.name()),
                Map.of("status", request.status().name()), null);
            if (task.getStatus() == TicketTaskStatus.COMPLETED && ticket.getClient() != null) {
                notificationService.notify(ticket.getClient().getUserAccount(), NotificationType.TICKET_TASK_COMPLETED,
                    "Tâche terminée",
                    "Une tâche de votre demande « " + ticket.getSubject() + " » a été terminée.",
                    "#/client/tickets/" + ticket.getPublicId());
            }
        }
        ticketTaskRepository.save(task);
        return buildDetail(ticketRepository.findById(ticket.getId()).orElse(ticket), actor);
    }

    @Transactional
    public TicketDetailResponse deleteTask(String email, UUID ticketPublicId, UUID taskPublicId) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        TicketTask task = ticketTaskRepository.findByPublicIdAndTicket_Id(taskPublicId, ticket.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable : " + taskPublicId));
        ticketTaskRepository.delete(task);
        auditService.record(AuditAction.TICKET_TASK_DELETED, "TICKET_TASK", task.getId(), task.getPublicId(),
            task.getTitle(), Map.of(), Map.of("ticketId", ticket.getPublicId()), null);
        return buildDetail(ticketRepository.findById(ticket.getId()).orElse(ticket), actor);
    }

    /**
     * Purge définitive d'un ticket par un administrateur : motif obligatoire,
     * action tracée en audit ; messages et tâches supprimés par cascade FK.
     */
    @Transactional
    public void deleteTicket(String email, UUID ticketPublicId, String reason) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        String normalizedReason = reason.trim();

        auditService.record(AuditAction.TICKET_DELETED, "TICKET", ticket.getId(), ticket.getPublicId(),
            ticket.getSubject(), Map.of(
                "status", ticket.getStatus().name(),
                "category", ticket.getCategory().name(),
                "priority", ticket.getPriority().name(),
                "clientCompanyName", ticket.getClientCompanyNameSnapshot()),
            Map.of("deletedBy", actor.getEmail(), "purged", true), normalizedReason);

        ticketTaskRepository.deleteAll(ticketTaskRepository.findAllByTicket_IdOrderByPositionAscCreatedAtAsc(ticket.getId()));
        ticketMessageRepository.deleteAll(ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(ticket.getId()));
        ticketRepository.delete(ticket);
    }

    @Transactional
    public com.documania.backend.ticket.dto.TicketAttachmentResponse uploadAttachment(String email, UUID ticketPublicId,
                                                                                       org.springframework.web.multipart.MultipartFile file) {
        UserAccount actor = user(email);
        Ticket ticket = ticketRepository.findByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        return attachmentService.upload(ticket, file, actor);
    }

    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(String email, UUID ticketPublicId,
                                                                                     UUID attachmentPublicId) {
        user(email);
        ticketRepository.findForReadByPublicId(ticketPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        var attachment = attachmentService.getAttachment(attachmentPublicId);
        if (!attachment.getTicket().getPublicId().equals(ticketPublicId)) {
            throw new BusinessRuleException("Cette pièce jointe n'appartient pas à ce ticket");
        }
        return attachmentService.download(attachmentPublicId);
    }

    private UserAccount user(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("Compte utilisateur introuvable"));
    }
}
