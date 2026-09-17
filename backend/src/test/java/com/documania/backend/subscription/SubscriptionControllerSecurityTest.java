package com.documania.backend.subscription;

import com.documania.backend.security.SecurityConfig;
import com.documania.backend.subscription.dto.SubscriptionPageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@Import(SecurityConfig.class)
class SubscriptionControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean SubscriptionQueryService queryService;
    @MockitoBean SubscriptionCommandService commandService;

    @Test void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/client/subscriptions")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToReadOwnSubscriptions() throws Exception {
        when(queryService.listForCurrentClient("client@test.local")).thenReturn(List.of());
        mockMvc.perform(get("/api/client/subscriptions")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "STAFF")
    void shouldRejectStaffWithoutReadPermission() throws Exception {
        mockMvc.perform(get("/api/staff/subscriptions")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities = {"ROLE_STAFF", "SUBSCRIPTION_READ"})
    void shouldAllowStaffWithReadPermission() throws Exception {
        when(queryService.listAll(isNull(), isNull(), eq(0), eq(20)))
            .thenReturn(new SubscriptionPageResponse(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/staff/subscriptions")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministrator() throws Exception {
        when(queryService.listAll(isNull(), isNull(), eq(0), eq(20)))
            .thenReturn(new SubscriptionPageResponse(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/staff/subscriptions")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldValidatePaginationBoundsOnStaffSubscriptionList() throws Exception {
        mockMvc.perform(get("/api/staff/subscriptions").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/subscriptions").param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/subscriptions").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username = "admin@test.local", roles = "ADMIN")
    void shouldAllowAdministratorToCancel() throws Exception {
        mockMvc.perform(patch("/api/staff/subscriptions/11111111-1111-1111-1111-111111111111/cancel")
                .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Résiliation demandée\"}"))
            .andExpect(status().isOk());
    }

    @Test @WithMockUser(authorities = {"ROLE_STAFF", "SUBSCRIPTION_CANCEL"})
    void shouldRejectStaffEvenWithCancellationAuthority() throws Exception {
        mockMvc.perform(patch("/api/staff/subscriptions/11111111-1111-1111-1111-111111111111/cancel")
                .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Résiliation demandée\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldRejectBlankCancellationReason() throws Exception {
        mockMvc.perform(patch("/api/staff/subscriptions/11111111-1111-1111-1111-111111111111/cancel")
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
            .andExpect(status().isBadRequest());
    }
}
