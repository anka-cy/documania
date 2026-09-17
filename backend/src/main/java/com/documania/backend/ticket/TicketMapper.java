package com.documania.backend.ticket;

import com.documania.backend.ticket.dto.TicketAttachmentResponse;
import com.documania.backend.ticket.dto.TicketMessageResponse;
import com.documania.backend.ticket.dto.TicketResponse;
import com.documania.backend.ticket.dto.TicketTaskResponse;
import com.documania.backend.user.UserAccount;

import java.util.List;

public final class TicketMapper {
    private TicketMapper() {}

    public static TicketResponse toResponse(Ticket ticket, int unreadCount) {
        String orderNumber = ticket.getOrder() == null ? null : ticket.getOrder().getOrderNumber();
        String subscriptionServiceName =
            ticket.getSubscription() == null ? null : ticket.getSubscription().getServiceNameSnapshot();
        UserAccount assignedTo = ticket.getAssignedTo();
        String assignedToName = assignedTo == null ? null
            : assignedTo.getFirstName() + " " + assignedTo.getLastName();
        return new TicketResponse(
            ticket.getPublicId(),
            ticket.getClient() == null ? null : ticket.getClient().getPublicId(),
            ticket.getClientCompanyNameSnapshot(),
            ticket.getOrder() == null ? null : ticket.getOrder().getPublicId(),
            orderNumber,
            ticket.getSubscription() == null ? null : ticket.getSubscription().getPublicId(),
            subscriptionServiceName,
            assignedTo == null ? null : assignedTo.getPublicId(),
            assignedTo == null ? null : assignedTo.getEmail(),
            assignedToName,
            ticket.getCategory(),
            ticket.getPriority(),
            ticket.getStatus(),
            ticket.getSubject(),
            ticket.getDescription(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getClosedAt(),
            unreadCount
        );
    }

    public static TicketMessageResponse toMessageResponse(TicketMessage message, List<TicketAttachmentResponse> attachments) {
        UserAccount author = message.getAuthor();
        String role = author.getRole() == null ? null : author.getRole().getName().name();
        return new TicketMessageResponse(
            message.getPublicId(),
            author.getPublicId(),
            author.getEmail(),
            author.getFirstName() + " " + author.getLastName(),
            role,
            message.getMessage(),
            message.isInternal(),
            message.getCreatedAt(),
            attachments
        );
    }

    public static TicketAttachmentResponse toAttachmentResponse(TicketAttachment attachment) {
        return new TicketAttachmentResponse(
            attachment.getPublicId(),
            attachment.getTicket().getPublicId(),
            attachment.getFileName(),
            attachment.getMimeType(),
            attachment.getFileSize(),
            attachment.getUploadedBy().getPublicId(),
            attachment.getUploadedBy().getEmail(),
            attachment.getCreatedAt()
        );
    }

    public static TicketTaskResponse toTaskResponse(TicketTask task) {
        return new TicketTaskResponse(
            task.getPublicId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPosition(),
            task.getCompletedAt(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
