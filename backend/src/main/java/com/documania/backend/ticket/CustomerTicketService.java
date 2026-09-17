package com.documania.backend.ticket;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.notification.NotificationType;
import com.documania.backend.role.RoleName;
import com.documania.backend.subscription.Subscription;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.ticket.dto.AddTicketMessageRequest;
import com.documania.backend.ticket.dto.CreateTicketRequest;
import com.documania.backend.ticket.dto.TicketDetailResponse;
import com.documania.backend.ticket.dto.TicketMessageResponse;
import com.documania.backend.ticket.dto.TicketResponse;
import com.documania.backend.ticket.dto.TicketTaskResponse;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerTicketService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketTaskRepository ticketTaskRepository;
    private final ClientRepository clientRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationService notificationService;
    private final TicketReadMarkRepository readMarkRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketAttachmentService attachmentService;

    public CustomerTicketService(TicketRepository ticketRepository,
                                 TicketMessageRepository ticketMessageRepository,
                                 TicketTaskRepository ticketTaskRepository,
                                 ClientRepository clientRepository,
                                 SubscriptionRepository subscriptionRepository,
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
        this.clientRepository = clientRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationService = notificationService;
        this.readMarkRepository = readMarkRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentService = attachmentService;
    }

    public List<TicketResponse> listForCurrentClient(String email) {
        Client client = activeClient(email);
        List<Ticket> tickets = ticketRepository.findAllByClient_IdOrderByCreatedAtDesc(client.getId());
        Map<Long, Integer> unreadByTicket = unreadCounts(client.getUserAccount().getId(),
            tickets.stream().map(Ticket::getId).toList());
        return tickets.stream()
            .map(ticket -> TicketMapper.toResponse(ticket,
                unreadByTicket.getOrDefault(ticket.getId(), 0)))
            .toList();
    }

    public TicketDetailResponse detailForCurrentClient(String email, UUID ticketPublicId) {
        Client client = activeClient(email);
        Ticket ticket = ticketRepository.findForReadByPublicIdAndClient_Id(ticketPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        return buildDetail(ticket, client.getUserAccount().getId());
    }

    /** Marque la discussion du ticket comme lue pour le client courant. */
    @Transactional
    public TicketDetailResponse markRead(String email, UUID ticketPublicId) {
        Client client = activeClient(email);
        Ticket ticket = ticketRepository.findForReadByPublicIdAndClient_Id(ticketPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        Long userId = client.getUserAccount().getId();
        TicketReadMark mark = readMarkRepository.findByTicket_IdAndUser_Id(ticket.getId(), userId)
            .orElseGet(() -> new TicketReadMark(ticket, client.getUserAccount(), LocalDateTime.now()));
        mark.markRead(LocalDateTime.now());
        readMarkRepository.save(mark);
        return buildDetail(ticket, userId);
    }

    /** Nombre de messages non lus par ticket pour un utilisateur (une requête). */
    private Map<Long, Integer> unreadCounts(Long userId, List<Long> ticketIds) {
        Map<Long, Integer> result = new java.util.HashMap<>();
        if (ticketIds.isEmpty()) return result;
        List<Object[]> rows = readMarkRepository.countUnreadForTickets(userId, ticketIds);
        if (rows == null) return result;
        for (Object[] row : rows) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return result;
    }

    private TicketDetailResponse buildDetail(Ticket ticket, Long userId) {
        List<TicketMessage> messages = ticketMessageRepository
            .findAllByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        List<TicketMessageResponse> messageResponses = messages.stream()
            .filter(message -> !message.isInternal())
            .map(message -> TicketMapper.toMessageResponse(message,
                attachmentResponsesForMessage(message)))
            .toList();
        List<TicketTaskResponse> tasks = ticketTaskRepository
            .findAllByTicket_IdOrderByPositionAscCreatedAtAsc(ticket.getId()).stream()
            .map(TicketMapper::toTaskResponse).toList();
        int progressPercent = computeProgressPercent(tasks);
        int unread = 0;
        if (ticket.getId() != null) {
            unread = unreadCounts(userId, List.of(ticket.getId())).getOrDefault(ticket.getId(), 0);
        }
        return new TicketDetailResponse(TicketMapper.toResponse(ticket, unread), messageResponses, tasks, progressPercent);
    }

    private List<com.documania.backend.ticket.dto.TicketAttachmentResponse> attachmentResponsesForMessage(TicketMessage message) {
        if (message.getId() == null) return List.of();
        return attachmentRepository.findAllByMessage_IdOrderByCreatedAtAsc(message.getId()).stream()
            .map(TicketMapper::toAttachmentResponse).toList();
    }

    static int computeProgressPercent(List<TicketTaskResponse> tasks) {
        if (tasks.isEmpty()) return 0;
        long completed = tasks.stream().filter(task -> task.status() == TicketTaskStatus.COMPLETED).count();
        return (int) Math.round((completed * 100.0) / tasks.size());
    }

    @Transactional
    public TicketDetailResponse createForCurrentClient(String email, CreateTicketRequest request) {
        Client client = activeClient(email);
        UserAccount author = client.getUserAccount();

        Subscription subscription = null;
        if (request.subscriptionId() != null) {
            subscription = subscriptionRepository.findByPublicId(request.subscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Abonnement introuvable : " + request.subscriptionId()));
            if (!subscription.getClient().getId().equals(client.getId())) {
                throw new BusinessRuleException("Cet abonnement ne vous appartient pas");
            }
            if (request.category() == TicketCategory.MODULE_CUSTOMIZATION
                || request.category() == TicketCategory.SERVICE_DOWN) {
                validateActiveSubscription(subscription);
            }
        }

        String subject = request.subject().trim();
        String description = request.description().trim();
        Ticket ticket = new Ticket(client, null, subscription, request.category(), request.priority(),
            subject, description);
        Ticket saved = ticketRepository.save(ticket);

        ticketMessageRepository.save(new TicketMessage(saved, author,
            "Ticket ouvert par " + client.getCompanyName() + ". " + description, false));

        auditService.record(AuditAction.TICKET_CREATED, "TICKET", saved.getId(), saved.getPublicId(),
            saved.getSubject(), Map.of(), Map.of(
                "category", saved.getCategory().name(),
                "priority", saved.getPriority().name(),
                "status", saved.getStatus().name(),
                "clientCompanyName", saved.getClientCompanyNameSnapshot()), null);

        String ticketReference = ticketReference(saved);
        EmailService.EmailMessage openedMessage =
            emailService.buildTicketOpenedMessage(author.getEmail(), saved.getSubject(), ticketReference);
        emailOutboxRepository.save(new EmailOutbox(
            openedMessage.recipient(), openedMessage.subject(), openedMessage.body()));

        // Notifie le staff/admin qu'un nouveau ticket est ouvert.
        String staffMessage = "Nouveau ticket « " + saved.getSubject() + " » de "
            + saved.getClientCompanyNameSnapshot() + ".";
        for (RoleName role : List.of(RoleName.STAFF, RoleName.ADMIN)) {
            for (UserAccount staff : userAccountRepository.findAllByRole_NameOrderByCreatedAtDesc(role)) {
                if (staff.isEnabled() && staff.isEmailVerified()) {
                    notificationService.notify(staff, NotificationType.TICKET_OPENED,
                        "Nouveau ticket", staffMessage, "#/staff/tickets/" + saved.getPublicId());
                }
            }
        }

        return buildDetail(saved, client.getUserAccount().getId());
    }

    @Transactional
    public TicketDetailResponse addClientMessage(String email, UUID ticketPublicId, AddTicketMessageRequest request) {
        Client client = activeClient(email);
        Ticket ticket = ticketRepository.findByPublicIdAndClient_Id(ticketPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Ce ticket est clôturé : vous ne pouvez plus y répondre");
        }
        List<UUID> attachmentIds = request.attachmentIds() == null ? List.of() : request.attachmentIds();
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isBlank() && attachmentIds.isEmpty()) {
            throw new BusinessRuleException("Un message ou une pièce jointe est requis");
        }
        TicketMessage savedMessage = ticketMessageRepository.save(
            new TicketMessage(ticket, client.getUserAccount(), message, false));

        // Lie les pièces jointes pré-uploadées au message.
        attachmentService.linkToMessage(savedMessage, attachmentIds);

        // Un message du client repasse automatiquement le ticket en cours.
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.changeStatus(TicketStatus.IN_PROGRESS, LocalDateTime.now());
            ticketRepository.save(ticket);
        }

        // Notifie le staff assigné (ou tout le staff) d'un nouveau message client.
        String staffMessage = "Nouveau message de " + client.getCompanyName()
            + " sur « " + ticket.getSubject() + " ».";
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().isEnabled()) {
            notificationService.notify(ticket.getAssignedTo(), NotificationType.TICKET_MESSAGE,
                "Nouveau message client", staffMessage, "#/staff/tickets/" + ticket.getPublicId());
        } else {
            for (RoleName role : List.of(RoleName.STAFF, RoleName.ADMIN)) {
                for (UserAccount staff : userAccountRepository.findAllByRole_NameOrderByCreatedAtDesc(role)) {
                    if (staff.isEnabled() && staff.isEmailVerified()) {
                        notificationService.notify(staff, NotificationType.TICKET_MESSAGE,
                            "Nouveau message client", staffMessage, "#/staff/tickets/" + ticket.getPublicId());
                    }
                }
            }
        }

        return buildDetail(ticketRepository.findById(ticket.getId()).orElse(ticket),
            client.getUserAccount().getId());
    }

    @Transactional
    public TicketDetailResponse closeForCurrentClient(String email, UUID ticketPublicId, String reason) {
        Client client = activeClient(email);
        Ticket ticket = ticketRepository.findByPublicIdAndClient_Id(ticketPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Ce ticket est déjà clôturé");
        }
        LocalDateTime now = LocalDateTime.now();
        TicketStatus previousStatus = ticket.getStatus();
        ticket.changeStatus(TicketStatus.CLOSED, now);
        Ticket saved = ticketRepository.save(ticket);

        if (reason != null && !reason.isBlank()) {
            ticketMessageRepository.save(new TicketMessage(saved, client.getUserAccount(),
                "Ticket clôturé par le client. " + reason.trim(), false));
        }

        auditService.record(AuditAction.TICKET_CLOSED, "TICKET", saved.getId(), saved.getPublicId(),
            saved.getSubject(), Map.of("status", previousStatus.name()),
            Map.of("status", TicketStatus.CLOSED.name(), "closedAt", now), reason);

        return buildDetail(saved, client.getUserAccount().getId());
    }

    @Transactional
    public com.documania.backend.ticket.dto.TicketAttachmentResponse uploadAttachment(String email, UUID ticketPublicId,
                                                                                       org.springframework.web.multipart.MultipartFile file) {
        Client client = activeClient(email);
        Ticket ticket = ticketRepository.findByPublicIdAndClient_Id(ticketPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        return attachmentService.upload(ticket, file, client.getUserAccount());
    }

    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(String email, UUID ticketPublicId,
                                                                                     UUID attachmentPublicId) {
        Client client = activeClient(email);
        ticketRepository.findForReadByPublicIdAndClient_Id(ticketPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + ticketPublicId));
        var attachment = attachmentService.getAttachment(attachmentPublicId);
        if (!attachment.getTicket().getPublicId().equals(ticketPublicId)) {
            throw new com.documania.backend.common.exception.BusinessRuleException(
                "Cette pièce jointe n'appartient pas à ce ticket");
        }
        return attachmentService.download(attachmentPublicId);
    }

    private void validateActiveSubscription(Subscription subscription) {
        if (subscription.getStatus() != com.documania.backend.subscription.SubscriptionStatus.ACTIVE) {
            throw new BusinessRuleException(
                "Cette action nécessite un abonnement actif");
        }
    }

    private Client activeClient(String email) {
        return clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived(email, false)
            .orElseThrow(() -> new ResourceNotFoundException("Compte client actif introuvable"));
    }

    static String ticketReference(Ticket ticket) {
        return "TKT-" + ticket.getPublicId().toString().substring(0, 8).toUpperCase();
    }
}
