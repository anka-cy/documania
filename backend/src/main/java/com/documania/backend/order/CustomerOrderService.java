package com.documania.backend.order;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.offer.Offer;
import com.documania.backend.offer.OfferRepository;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import com.documania.backend.role.RoleName;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.notification.NotificationType;
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
import com.documania.backend.ticket.TicketStatus;
import com.documania.backend.order.dto.OrderPageResponse;
import com.documania.backend.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerOrderService {
    private final CustomerOrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final OfferRepository offerRepository;
    private final AuditService auditService;
    private final UserAccountRepository userAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository subscriptionPeriodRepository;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationService notificationService;
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;

    public CustomerOrderService(CustomerOrderRepository orderRepository, ClientRepository clientRepository,
                                OfferRepository offerRepository, AuditService auditService,
                                UserAccountRepository userAccountRepository,
                                SubscriptionRepository subscriptionRepository,
                                SubscriptionPeriodRepository subscriptionPeriodRepository,
                                EmailService emailService,
                                EmailOutboxRepository emailOutboxRepository,
                                NotificationService notificationService,
                                TicketRepository ticketRepository,
                                TicketMessageRepository ticketMessageRepository) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.offerRepository = offerRepository;
        this.auditService = auditService;
        this.userAccountRepository = userAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPeriodRepository = subscriptionPeriodRepository;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationService = notificationService;
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
    }

    @Transactional
    public CustomerOrder createForCurrentClient(String email, UUID offerPublicId) {
        Client client = activeClient(email);
        CustomerOrder order = createOrder(client, offerPublicId);

        // Une seule requête des destinataires éligibles + un seul lot d'insertions.
        String staffMessage = "Nouvelle commande " + order.getOrderNumber()
            + " de " + order.getClientCompanyNameSnapshot() + " (« " + order.getOfferNameSnapshot() + " »).";
        List<UserAccount> recipients = userAccountRepository
            .findAllByRole_NameInAndEnabledTrueAndEmailVerifiedTrue(List.of(RoleName.STAFF, RoleName.ADMIN));
        notificationService.notifyAll(recipients, NotificationType.NEW_ORDER_PENDING,
            "Nouvelle commande en attente", staffMessage, "#/staff/orders");
        return order;
    }

    @Transactional
    public ConfirmedOrder createAndConfirmForClient(String administratorEmail, UUID clientPublicId, UUID offerPublicId) {
        Client client = clientRepository.findByPublicIdAndArchived(clientPublicId, false)
            .orElseThrow(() -> new ResourceNotFoundException("Client actif introuvable : " + clientPublicId));
        CustomerOrder order = createOrder(client, offerPublicId);
        return confirm(administratorEmail, order.getPublicId());
    }

    private CustomerOrder createOrder(Client client, UUID offerPublicId) {
        Offer offer = offerRepository.findByPublicIdAndArchived(offerPublicId, false)
            .orElseThrow(() -> new ResourceNotFoundException("Offre active introuvable : " + offerPublicId));
        LocalDate today = LocalDate.now();
        if (today.isBefore(offer.getCommercialStartDate()) || today.isAfter(offer.getCommercialEndDate())) {
            throw new BusinessRuleException("Cette offre n'est pas disponible commercialement aujourd'hui");
        }

        Long serviceId = offer.getCatalogService().getId();
        if (subscriptionRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(
            client.getId(), SubscriptionStatus.ACTIVE, serviceId)) {
            throw new BusinessRuleException(
                "Ce client possède déjà un abonnement actif pour ce service");
        }
        if (orderRepository.existsByClient_IdAndStatusAndOffer_CatalogService_Id(
            client.getId(), OrderStatus.PENDING, serviceId)) {
            throw new BusinessRuleException(
                "Une commande pour ce service est déjà en attente de validation");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        CustomerOrder saved = orderRepository.save(new CustomerOrder(client, offer, orderNumber));
        auditService.record(AuditAction.ORDER_CREATED, "ORDER", saved.getId(), saved.getPublicId(),
            saved.getOrderNumber(), Map.of(), Map.of(
                "status", saved.getStatus().name(),
                "clientCompanyName", saved.getClientCompanyNameSnapshot(),
                "offerName", saved.getOfferNameSnapshot(),
                    "price", saved.getPriceSnapshot()
                ), null);

        createOrderRequirementsTicket(saved);

        return saved;
    }

    /**
     * Toute commande en attente ouvre un ticket de recueil des exigences lié,
     * pour aligner client et staff sur les prérequis avant confirmation.
     */
    private void createOrderRequirementsTicket(CustomerOrder saved) {
        if (ticketRepository.existsByOrder_IdAndCategory(saved.getId(), TicketCategory.ORDER_REQUIREMENTS)) {
            return;
        }
        String description = "Commande " + saved.getOrderNumber()
            + " — Offre : « " + saved.getOfferNameSnapshot() + " » ("
            + saved.getServiceNameSnapshot() + "), montant " + saved.getPriceSnapshot().toPlainString()
            + " MAD, durée " + saved.getOfferDurationMonthsSnapshot() + " mois."
            + " Merci de confirmer les exigences techniques et fonctionnelles avant la validation de la commande.";
        Ticket ticket = ticketRepository.save(new Ticket(
            saved.getClient(), saved, null, TicketCategory.ORDER_REQUIREMENTS,
            com.documania.backend.ticket.TicketPriority.MEDIUM,
            "Exigences — Commande " + saved.getOrderNumber(), description));

        ticketMessageRepository.save(new TicketMessage(ticket, saved.getClient().getUserAccount(),
            "Ticket ouvert automatiquement pour la commande " + saved.getOrderNumber()
                + ". Ce fil de discussion sert à valider les exigences avant la confirmation.", false));

        auditService.record(AuditAction.TICKET_CREATED, "TICKET", ticket.getId(), ticket.getPublicId(),
            ticket.getSubject(), Map.of(), Map.of(
                "category", ticket.getCategory().name(),
                "status", ticket.getStatus().name(),
                "orderNumber", saved.getOrderNumber()), null);
    }

    /** Clôture le ticket d'exigences lié à la commande, avec message automatique. */
    private void closeOrderRequirementsTicket(CustomerOrder order, String statusLabel) {
        ticketRepository.findByOrder_IdAndCategory(order.getId(), TicketCategory.ORDER_REQUIREMENTS)
            .ifPresent(ticket -> {
                if (ticket.getStatus() != TicketStatus.CLOSED) {
                    ticket.changeStatus(TicketStatus.CLOSED, LocalDateTime.now());
                    ticketRepository.save(ticket);
                    ticketMessageRepository.save(new TicketMessage(ticket,
                        order.getClient().getUserAccount(),
                        "Ticket clôturé automatiquement : commande " + order.getOrderNumber()
                            + " " + statusLabel + ".", false));
                }
            });
    }

    public List<CustomerOrder> listForCurrentClient(String email) {
        return orderRepository.findAllByClient_IdOrderByCreatedAtDesc(activeClient(email).getId());
    }

    /** Le mot-clé de recherche est normalisé (trim + minuscules) côté service. */
    public OrderPageResponse listAll(String query, OrderStatus status, int page, int size) {
        String normalized = query == null || query.isBlank() ? null : query.trim().toLowerCase();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CustomerOrder> result = orderRepository.search(status, normalized, pageRequest);
        List<OrderResponse> items = result.getContent().stream()
            .map(CustomerOrderMapper::toResponse)
            .toList();
        return new OrderPageResponse(items, result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public CustomerOrder cancelForCurrentClient(String email, UUID orderPublicId) {
        Client client = activeClient(email);
        CustomerOrder order = orderRepository.findByPublicIdAndClient_Id(orderPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderPublicId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Seule une commande en attente peut être annulée");
        }

        order.cancel(LocalDateTime.now());
        CustomerOrder saved = orderRepository.save(order);
        auditService.record(AuditAction.ORDER_CANCELLED, "ORDER", saved.getId(), saved.getPublicId(),
            saved.getOrderNumber(), Map.of("status", OrderStatus.PENDING.name()),
            Map.of("status", OrderStatus.CANCELLED.name(), "cancelledAt", saved.getCancelledAt()), null);

        closeOrderRequirementsTicket(saved, "annulée par le client");

        return saved;
    }

    @Transactional
    public CustomerOrder reject(String administratorEmail, UUID orderPublicId, String reason) {
        CustomerOrder order = orderRepository.findByPublicId(orderPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderPublicId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Seule une commande en attente peut être rejetée");
        }
        UserAccount administrator = userAccountRepository.findByEmailIgnoreCase(administratorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Compte administrateur introuvable"));
        String normalizedReason = reason.trim();
        order.reject(normalizedReason, administrator, LocalDateTime.now());
        CustomerOrder saved = orderRepository.save(order);
        auditService.record(AuditAction.ORDER_REJECTED, "ORDER", saved.getId(), saved.getPublicId(),
            saved.getOrderNumber(), Map.of("status", OrderStatus.PENDING.name()),
            Map.of("status", OrderStatus.REJECTED.name(), "rejectionReason", normalizedReason,
                "processedAt", saved.getProcessedAt()), normalizedReason);

        closeOrderRequirementsTicket(saved, "rejetée (motif : " + normalizedReason + ")");

        EmailService.EmailMessage message = emailService.buildOrderRejectedMessage(
            order.getClient().getUserAccount().getEmail(),
            saved.getOrderNumber(),
            normalizedReason
        );
        emailOutboxRepository.save(new EmailOutbox(message.recipient(), message.subject(), message.body()));

        notificationService.notify(order.getClient().getUserAccount(), NotificationType.ORDER_REJECTED,
            "Commande rejetée",
            "Votre commande " + saved.getOrderNumber() + " a été rejetée. Motif : " + normalizedReason,
            "#/client/orders");
        return saved;
    }

    @Transactional
    public ConfirmedOrder confirm(String administratorEmail, UUID orderPublicId) {
        CustomerOrder order = orderRepository.findByPublicId(orderPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderPublicId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Seule une commande en attente peut être confirmée");
        }
        if (subscriptionRepository.existsByCustomerOrder_Id(order.getId())) {
            throw new BusinessRuleException("Cette commande possède déjà un abonnement");
        }
        UserAccount administrator = userAccountRepository.findByEmailIgnoreCase(administratorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Compte administrateur introuvable"));

        LocalDateTime processedAt = LocalDateTime.now();
        LocalDate startDate = processedAt.toLocalDate();
        LocalDate endDate = startDate.plusMonths(order.getOfferDurationMonthsSnapshot()).minusDays(1);
        order.confirm(administrator, processedAt);
        CustomerOrder savedOrder = orderRepository.save(order);
        Subscription subscription = subscriptionRepository.save(new Subscription(
            order.getClient(), order.getOffer(), order, startDate, endDate));
        SubscriptionPeriod period = subscriptionPeriodRepository.save(new SubscriptionPeriod(
            subscription, order, administrator, startDate, endDate));

        auditService.record(AuditAction.ORDER_CONFIRMED, "ORDER", savedOrder.getId(), savedOrder.getPublicId(),
            savedOrder.getOrderNumber(), Map.of("status", OrderStatus.PENDING.name()),
            Map.of("status", OrderStatus.CONFIRMED.name(), "processedAt", processedAt), null);
        auditService.record(AuditAction.SUBSCRIPTION_CREATED, "SUBSCRIPTION", subscription.getId(),
            subscription.getPublicId(), savedOrder.getOrderNumber(), Map.of(),
            Map.of("status", subscription.getStatus().name(), "startDate", startDate,
                "endDate", endDate, "orderNumber", savedOrder.getOrderNumber()), null);

        String clientEmail = order.getClient().getUserAccount().getEmail();
        EmailService.EmailMessage confirmedMessage = emailService.buildOrderConfirmedMessage(
            clientEmail, savedOrder.getOrderNumber(), savedOrder.getOfferNameSnapshot(),
            startDate, endDate);
        emailOutboxRepository.save(new EmailOutbox(
            confirmedMessage.recipient(), confirmedMessage.subject(), confirmedMessage.body()));
        EmailService.EmailMessage activatedMessage = emailService.buildSubscriptionActivatedMessage(
            clientEmail, order.getClient().getCompanyName(), savedOrder.getOfferNameSnapshot(),
            startDate, endDate);
        emailOutboxRepository.save(new EmailOutbox(
            activatedMessage.recipient(), activatedMessage.subject(), activatedMessage.body()));

        notificationService.notify(order.getClient().getUserAccount(), NotificationType.ORDER_CONFIRMED,
            "Commande confirmée",
            "Votre commande " + savedOrder.getOrderNumber() + " est confirmée. Votre abonnement est actif du "
                + startDate + " au " + endDate + ".",
            "#/client/subscriptions");

        closeOrderRequirementsTicket(savedOrder, "confirmée");

        return new ConfirmedOrder(savedOrder, subscription, period);
    }

    public record ConfirmedOrder(CustomerOrder order, Subscription subscription, SubscriptionPeriod period) {}

    /** Montant et numéro de facture calculés côté backend ; le frontend n'affiche que les valeurs reçues. */
    @Transactional(readOnly = true)
    public com.documania.backend.order.dto.InvoiceResponse findInvoiceForCurrentClient(String email, UUID orderPublicId) {
        CustomerOrder order = confirmedOrderOf(email, orderPublicId);
        BigDecimal price = order.getPriceSnapshot();
        return new com.documania.backend.order.dto.InvoiceResponse(
            "FAC-" + order.getOrderNumber(),
            order.getOrderNumber(),
            order.getClientCompanyNameSnapshot(),
            order.getOfferNameSnapshot(),
            order.getServiceNameSnapshot(),
            order.getOfferDurationMonthsSnapshot(),
            price,
            order.getCreatedAt()
        );
    }

    private CustomerOrder confirmedOrderOf(String email, UUID orderPublicId) {
        Client client = activeClient(email);
        CustomerOrder order = orderRepository.findForInvoiceByPublicIdAndClient_Id(orderPublicId, client.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable pour ce client"));
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessRuleException(
                "La facture n'est disponible que pour une commande confirmée"
            );
        }
        return order;
    }

    private Client activeClient(String email) {
        return clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived(email, false)
            .orElseThrow(() -> new ResourceNotFoundException("Compte client actif introuvable"));
    }
}
