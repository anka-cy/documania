package com.documania.backend.subscription;

import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.subscription.dto.SubscriptionPageResponse;
import com.documania.backend.subscription.dto.SubscriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SubscriptionQueryService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPeriodRepository periodRepository;
    private final ClientRepository clientRepository;

    public SubscriptionQueryService(SubscriptionRepository subscriptionRepository,
                                    SubscriptionPeriodRepository periodRepository,
                                    ClientRepository clientRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.periodRepository = periodRepository;
        this.clientRepository = clientRepository;
    }

    public List<SubscriptionResponse> listForCurrentClient(String email) {
        Client client = clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived(email, false)
            .orElseThrow(() -> new ResourceNotFoundException("Compte client actif introuvable"));
        return mapWithPeriods(subscriptionRepository.findAllByClient_IdOrderByCreatedAtDesc(client.getId()));
    }

    /** Périodes chargées en un seul lot limité à la page courante (évite le N+1). */
    public SubscriptionPageResponse listAll(String query, SubscriptionStatus status, int page, int size) {
        String normalized = query == null || query.isBlank() ? null : query.trim().toLowerCase();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Subscription> result = subscriptionRepository.search(status, normalized, pageRequest);
        List<SubscriptionResponse> items = mapWithPeriods(result.getContent());
        return new SubscriptionPageResponse(items, result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    private List<SubscriptionResponse> mapWithPeriods(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) return List.of();
        List<Long> ids = subscriptions.stream().map(Subscription::getId).toList();
        Map<Long, List<SubscriptionPeriod>> periodsBySubscription = new HashMap<>();
        for (SubscriptionPeriod period : periodRepository
            .findAllBySubscription_IdInOrderBySubscription_IdAscPeriodNumberAsc(ids)) {
            periodsBySubscription.computeIfAbsent(period.getSubscription().getId(), ignored -> new ArrayList<>())
                .add(period);
        }
        return subscriptions.stream().map(subscription -> SubscriptionMapper.toResponse(
            subscription, periodsBySubscription.getOrDefault(subscription.getId(), List.of()))).toList();
    }
}
