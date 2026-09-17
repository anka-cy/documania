package com.documania.backend.export;

import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExportController.class)
@Import(SecurityConfig.class)
class ExportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

    @Test
    void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/staff/export/clients"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffWithoutExportPermission() throws Exception {
        mockMvc.perform(get("/api/staff/export/clients"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/export/orders"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/export/subscriptions"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_EXPORT"})
    void shouldAllowStaffWithClientExportPermissionForXlsx() throws Exception {
        when(exportService.exportClients()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/staff/export/clients"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"clients.xlsx\""));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "ORDER_EXPORT"})
    void shouldAllowStaffWithOrderExportPermission() throws Exception {
        when(exportService.exportOrders()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/staff/export/orders"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"orders.xlsx\""));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "SUBSCRIPTION_EXPORT"})
    void shouldAllowStaffWithSubscriptionExportPermission() throws Exception {
        when(exportService.exportSubscriptions()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/staff/export/subscriptions"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"subscriptions.xlsx\""));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToExportAll() throws Exception {
        when(exportService.exportClients()).thenReturn(new byte[]{1});
        when(exportService.exportOrders()).thenReturn(new byte[]{1});
        when(exportService.exportSubscriptions()).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/staff/export/clients")).andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/export/orders")).andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/export/subscriptions")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRejectClientFromStaffExports() throws Exception {
        mockMvc.perform(get("/api/staff/export/clients"))
            .andExpect(status().isForbidden());
    }
}