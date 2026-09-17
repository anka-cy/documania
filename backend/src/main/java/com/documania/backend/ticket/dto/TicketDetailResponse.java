package com.documania.backend.ticket.dto;

import java.util.List;

public record TicketDetailResponse(
    TicketResponse ticket,
    List<TicketMessageResponse> messages,
    List<TicketTaskResponse> tasks,
    int progressPercent
) {}
