package com.documania.backend.dashboard;

import com.documania.backend.audit.AuditLogRepository;
import com.documania.backend.catalog.CatalogServiceRepository;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.dashboard.dto.DashboardSummaryResponse;
import com.documania.backend.offer.OfferRepository;
import com.documania.backend.order.CustomerOrderRepository;
import com.documania.backend.order.OrderStatus;
import com.documania.backend.subscription.SubscriptionRepository;
import com.documania.backend.subscription.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private ClientRepository clientRepository;
    private CatalogServiceRepository catalogServiceRepository;
    private OfferRepository offerRepository;
    private CustomerOrderRepository customerOrderRepository;
    private SubscriptionRepository subscriptionRepository;
    private AuditLogRepository auditLogRepository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        catalogServiceRepository = mock(CatalogServiceRepository.class);
        offerRepository = mock(OfferRepository.class);
        customerOrderRepository = mock(CustomerOrderRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        service = new DashboardService(
            clientRepository, catalogServiceRepository, offerRepository,
            customerOrderRepository, subscriptionRepository, auditLogRepository
        );
    }

    @Test
    void shouldAssembleSummaryCountsFromRepositories() {
        when(clientRepository.count()).thenReturn(10L);
        when(clientRepository.countByArchived(true)).thenReturn(2L);

        when(catalogServiceRepository.count()).thenReturn(5L);
        when(catalogServiceRepository.countByArchived(true)).thenReturn(1L);

        when(offerRepository.count()).thenReturn(20L);
        when(offerRepository.countByArchived(true)).thenReturn(3L);

        when(customerOrderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(4L);
        when(customerOrderRepository.countByStatus(OrderStatus.CONFIRMED)).thenReturn(6L);
        when(customerOrderRepository.count()).thenReturn(13L);
        when(customerOrderRepository.sumPriceSnapshotByStatus(OrderStatus.CONFIRMED))
            .thenReturn(new BigDecimal("1500.00"));

        when(subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)).thenReturn(7L);
        when(subscriptionRepository.count()).thenReturn(10L);
        when(subscriptionRepository.countByStatusAndEndDateBefore(eq(SubscriptionStatus.ACTIVE), any()))
            .thenReturn(3L);

        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
            .thenReturn(Page.empty());

        DashboardSummaryResponse response = service.summary(true);

        assertEquals(10L, response.clients().total());
        assertEquals(8L, response.clients().active());
        assertEquals(2L, response.clients().archived());

        assertEquals(5L, response.services().total());
        assertEquals(4L, response.services().active());
        assertEquals(1L, response.services().archived());

        assertEquals(20L, response.offers().total());
        assertEquals(17L, response.offers().active());
        assertEquals(3L, response.offers().archived());

        assertEquals(13L, response.orders().total());
        assertEquals(4L, response.orders().pending());
        assertEquals(6L, response.orders().confirmed());

        assertEquals(10L, response.subscriptions().total());
        assertEquals(7L, response.subscriptions().active());
        assertEquals(3L, response.subscriptions().expiringSoon());

        assertEquals(0, new BigDecimal("1500.00").compareTo(response.confirmedRevenue()));
        assertTrue(response.recentAuditEvents().isEmpty());
    }
}