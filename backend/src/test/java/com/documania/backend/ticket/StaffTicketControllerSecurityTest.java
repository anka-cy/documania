package com.documania.backend.ticket;

import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffTicketController.class)
@Import(SecurityConfig.class)
class StaffTicketControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean StaffTicketService ticketService;
    @MockitoBean TicketAttachmentService attachmentService;
    @MockitoBean TicketStreamPublisher streamPublisher;

    private static final UUID TICKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test void shouldRejectAnonymousTicketRead() throws Exception {
        mockMvc.perform(get("/api/staff/tickets")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "STAFF")
    void shouldRejectStaffWithoutTicketReadPermission() throws Exception {
        mockMvc.perform(get("/api/staff/tickets")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities = {"ROLE_STAFF", "TICKET_READ"})
    void shouldAllowStaffWithTicketReadPermission() throws Exception {
        mockMvc.perform(get("/api/staff/tickets")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToReadAllTickets() throws Exception {
        mockMvc.perform(get("/api/staff/tickets")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "STAFF")
    void shouldRejectStaffDeletingTicket() throws Exception {
        mockMvc.perform(delete("/api/staff/tickets/" + TICKET_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Motif de suppression valide\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToDeleteTicket() throws Exception {
        mockMvc.perform(delete("/api/staff/tickets/" + TICKET_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Motif de suppression valide\"}"))
            .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldValidateRequiredDeleteReason() throws Exception {
        mockMvc.perform(delete("/api/staff/tickets/" + TICKET_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"  \"}"))
            .andExpect(status().isBadRequest());
    }
}