package com.documania.backend.subscription;

import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class SubscriptionQueryServiceTest {
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionPeriodRepository periodRepository;
    private ClientRepository clientRepository;
    private SubscriptionQueryService service;

    @BeforeEach void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        periodRepository = mock(SubscriptionPeriodRepository.class);
        clientRepository = mock(ClientRepository.class);
        service = new SubscriptionQueryService(subscriptionRepository, periodRepository, clientRepository);
    }

    @Test void shouldReadOnlyCurrentClientsSubscriptions() {
        Client client = mock(Client.class);
        when(client.getId()).thenReturn(42L);
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.of(client));
        when(subscriptionRepository.findAllByClient_IdOrderByCreatedAtDesc(42L)).thenReturn(List.of());

        assertEquals(List.of(), service.listForCurrentClient("client@test.local"));
        verify(subscriptionRepository).findAllByClient_IdOrderByCreatedAtDesc(42L);
        verifyNoInteractions(periodRepository);
    }

    @Test void shouldRejectMissingOrArchivedClient() {
        when(clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived("client@test.local", false))
            .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
            () -> service.listForCurrentClient("client@test.local"));
        verifyNoInteractions(subscriptionRepository, periodRepository);
    }

    @Test void shouldReadAllSubscriptionsForStaffPortalPaginated() {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn(7L);
        when(subscriptionRepository.search(isNull(), isNull(),
            eq(org.springframework.data.domain.PageRequest.of(0, 20,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(subscription),
                org.springframework.data.domain.PageRequest.of(0, 20), 1));
        when(periodRepository.findAllBySubscription_IdInOrderBySubscription_IdAscPeriodNumberAsc(List.of(7L)))
            .thenReturn(List.of());

        com.documania.backend.subscription.dto.SubscriptionPageResponse response =
            service.listAll(null, null, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test void shouldNormalizeSearchQuery() {
        when(subscriptionRepository.search(isNull(), eq("alpha"),
            any(org.springframework.data.domain.PageRequest.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service.listAll("  ALPHA  ", null, 0, 20);

        verify(subscriptionRepository).search(isNull(), eq("alpha"),
            any(org.springframework.data.domain.PageRequest.class));
    }
}
