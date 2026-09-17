package com.documania.backend.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface SubscriptionPeriodRepository extends JpaRepository<SubscriptionPeriod, Long> {
    @EntityGraph(attributePaths = {"subscription", "createdBy"})
    List<SubscriptionPeriod> findAllBySubscription_IdInOrderBySubscription_IdAscPeriodNumberAsc(List<Long> ids);
}
