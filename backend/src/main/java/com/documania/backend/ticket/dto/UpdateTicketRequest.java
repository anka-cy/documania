package com.documania.backend.ticket.dto;

import com.documania.backend.ticket.TicketPriority;

public record UpdateTicketRequest(
    TicketPriority priority
) {}