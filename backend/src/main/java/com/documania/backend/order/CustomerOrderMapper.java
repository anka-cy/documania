package com.documania.backend.order;

import com.documania.backend.order.dto.OrderResponse;

public final class CustomerOrderMapper {
    private CustomerOrderMapper() {}

    public static OrderResponse toResponse(CustomerOrder order) {
        return new OrderResponse(order.getPublicId(), order.getOrderNumber(),
            order.getClient() == null ? null : order.getClient().getPublicId(),
            order.getClientCompanyNameSnapshot(),
            order.getOffer() == null ? null : order.getOffer().getPublicId(), order.getStatus(),
            order.getPriceSnapshot(), order.getOfferNameSnapshot(), order.getOfferDurationMonthsSnapshot(),
            order.getOfferNumberOfUsersSnapshot(), order.getServiceNameSnapshot(), order.getRejectionReason(),
            order.getProcessedBy() == null ? null : order.getProcessedBy().getPublicId(),
            order.getProcessedBy() == null ? null : order.getProcessedBy().getEmail(),
            order.getProcessedAt(), order.getCancelledAt(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
