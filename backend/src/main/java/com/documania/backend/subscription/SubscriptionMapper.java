package com.documania.backend.subscription;

import com.documania.backend.subscription.dto.SubscriptionResponse;
import com.documania.backend.subscription.dto.SubscriptionPeriodResponse;
import java.util.List;

public final class SubscriptionMapper {
    private SubscriptionMapper() {}
    public static SubscriptionResponse toResponse(Subscription value, List<SubscriptionPeriod> periods) {
        List<SubscriptionPeriodResponse> periodResponses = periods.stream().map(period ->
            new SubscriptionPeriodResponse(period.getPeriodNumber(), period.getStartDate(), period.getEndDate(),
                period.getPriceSnapshot(), period.getOfferNameSnapshot(), period.getOfferDurationMonthsSnapshot(),
                period.getOfferNumberOfUsersSnapshot(), period.getServiceNameSnapshot(),
                period.getCreatedBy() == null ? null : period.getCreatedBy().getPublicId(),
                period.getCreatedBy() == null ? null : period.getCreatedBy().getEmail(),
                period.getCreatedAt())).toList();
        return new SubscriptionResponse(value.getPublicId(),
            value.getClient() == null ? null : value.getClient().getPublicId(),
            value.getClientCompanyNameSnapshot(),
            value.getOffer() == null ? null : value.getOffer().getPublicId(),
            value.getOfferNameSnapshot(), value.getServiceNameSnapshot(),
            value.getCustomerOrder() == null ? null : value.getCustomerOrder().getPublicId(), value.getStatus(),
            value.getStartDate(), value.getEndDate(), value.getCancellationReason(), value.getCancelledAt(),
            value.getCancelledBy() == null ? null : value.getCancelledBy().getPublicId(),
            periodResponses, value.getCreatedAt(), value.getUpdatedAt());
    }
}
