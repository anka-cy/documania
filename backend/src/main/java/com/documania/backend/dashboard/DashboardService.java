package com.documania.backend.dashboard;

import com.documania.backend.audit.AuditLogRepository;
import com.documania.backend.audit.AuditMapper;
import com.documania.backend.catalog.CatalogServiceRepository;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.dashboard.dto.DashboardSummaryResponse;
import com.documania.backend.offer.OfferRepository;
import com.documania.backend.order.CustomerOrderRepository;
import com.documania.backend.order.OrderStatus;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.subscription.SubscriptionStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private static final int RECENT_AUDIT_SIZE = 5;
    private static final int EXPIRING_SOON_DAYS = 30;

    private final ClientRepository clientRepository;
    private final CatalogServiceRepository catalogServiceRepository;
    private final OfferRepository offerRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(
        ClientRepository clientRepository,
        CatalogServiceRepository catalogServiceRepository,
        OfferRepository offerRepository,
        CustomerOrderRepository customerOrderRepository,
        SubscriptionRepository subscriptionRepository,
        AuditLogRepository auditLogRepository
    ) {
        this.clientRepository = clientRepository;
        this.catalogServiceRepository = catalogServiceRepository;
        this.offerRepository = offerRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(boolean includeAuditEvents) {
        long clientsTotal = clientRepository.count();
        long clientsArchived = clientRepository.countByArchived(true);

        long servicesTotal = catalogServiceRepository.count();
        long servicesArchived = catalogServiceRepository.countByArchived(true);

        long offersTotal = offerRepository.count();
        long offersArchived = offerRepository.countByArchived(true);

        // Les totaux viennent de count() : les compteurs par statut n'incluent
        // pas tous les statuts (rejeté/annulé, expiré/annulé).
        long ordersPending = customerOrderRepository.countByStatus(OrderStatus.PENDING);
        long ordersConfirmed = customerOrderRepository.countByStatus(OrderStatus.CONFIRMED);
        long ordersTotal = customerOrderRepository.count();

        long subscriptionsActive = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long subscriptionsTotal = subscriptionRepository.count();
        long subscriptionsExpiringSoon = subscriptionRepository.countByStatusAndEndDateBefore(
            SubscriptionStatus.ACTIVE,
            LocalDate.now().plusDays(EXPIRING_SOON_DAYS)
        );

        return new DashboardSummaryResponse(
            new DashboardSummaryResponse.EntityCounts(
                clientsTotal,
                clientsTotal - clientsArchived,
                clientsArchived
            ),
            new DashboardSummaryResponse.EntityCounts(
                servicesTotal,
                servicesTotal - servicesArchived,
                servicesArchived
            ),
            new DashboardSummaryResponse.EntityCounts(
                offersTotal,
                offersTotal - offersArchived,
                offersArchived
            ),
            new DashboardSummaryResponse.OrderCounts(
                ordersTotal,
                ordersPending,
                ordersConfirmed
            ),
            new DashboardSummaryResponse.SubscriptionCounts(
                subscriptionsTotal,
                subscriptionsActive,
                subscriptionsExpiringSoon
            ),
            customerOrderRepository.sumPriceSnapshotByStatus(OrderStatus.CONFIRMED),
            includeAuditEvents
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_AUDIT_SIZE))
                    .getContent()
                    .stream()
                    .map(AuditMapper::toResponse)
                    .toList()
                : List.of()
        );
    }
}