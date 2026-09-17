package com.documania.backend.audit;

import com.documania.backend.audit.dto.AuditPageResponse;
import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditQueryService auditQueryService;

    @Test
    void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/staff/audits"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "AUDIT_READ"})
    void shouldRejectStaffEvenWithAuditAuthority() throws Exception {
        mockMvc.perform(get("/api/staff/audits"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRejectClient() throws Exception {
        mockMvc.perform(get("/api/staff/audits"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorWithDefaultPagination() throws Exception {
        when(auditQueryService.search(isNull(), isNull(), isNull(), eq(0), eq(20)))
            .thenReturn(new AuditPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/staff/audits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20));

        verify(auditQueryService).search(isNull(), isNull(), isNull(), eq(0), eq(20));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToChoosePageAndSize() throws Exception {
        when(auditQueryService.search(isNull(), isNull(), isNull(), eq(2), eq(50)))
            .thenReturn(new AuditPageResponse(List.of(), 2, 50, 120, 3));

        mockMvc.perform(get("/api/staff/audits?page=2&size=50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(120));

        verify(auditQueryService).search(isNull(), isNull(), isNull(), eq(2), eq(50));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldForwardSearchAndFilterParameters() throws Exception {
        when(auditQueryService.search("ORD-", AuditAction.ORDER_CONFIRMED, "ORDER", 0, 20))
            .thenReturn(new AuditPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/staff/audits")
                .param("query", "ORD-")
                .param("action", "ORDER_CONFIRMED")
                .param("entityType", "ORDER"))
            .andExpect(status().isOk());

        verify(auditQueryService).search("ORD-", AuditAction.ORDER_CONFIRMED, "ORDER", 0, 20);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectPageLargerThanMaximum() throws Exception {
        mockMvc.perform(get("/api/staff/audits?size=101"))
            .andExpect(status().isBadRequest());
    }
}
