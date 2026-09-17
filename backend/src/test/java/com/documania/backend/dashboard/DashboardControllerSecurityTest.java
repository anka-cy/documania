package com.documania.backend.dashboard;

import com.documania.backend.dashboard.dto.DashboardSummaryResponse;
import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/staff/dashboard/summary"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffWithoutDashboardReadPermission() throws Exception {
        mockMvc.perform(get("/api/staff/dashboard/summary"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "DASHBOARD_READ"})
    void shouldAllowStaffWithDashboardReadPermission() throws Exception {
        when(dashboardService.summary(anyBoolean())).thenReturn(summary());

        mockMvc.perform(get("/api/staff/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clients.total").value(10))
            .andExpect(jsonPath("$.confirmedRevenue").value(1500.00))
            .andExpect(jsonPath("$.recentAuditEvents").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministrator() throws Exception {
        when(dashboardService.summary(anyBoolean())).thenReturn(summary());

        mockMvc.perform(get("/api/staff/dashboard/summary"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRejectClientFromStaffDashboard() throws Exception {
        mockMvc.perform(get("/api/staff/dashboard/summary"))
            .andExpect(status().isForbidden());
    }

    private DashboardSummaryResponse summary() {
        return new DashboardSummaryResponse(
            new DashboardSummaryResponse.EntityCounts(10, 8, 2),
            new DashboardSummaryResponse.EntityCounts(5, 4, 1),
            new DashboardSummaryResponse.EntityCounts(20, 17, 3),
            new DashboardSummaryResponse.OrderCounts(13, 4, 6),
            new DashboardSummaryResponse.SubscriptionCounts(10, 7, 3),
            new BigDecimal("1500.00"),
            List.of()
        );
    }
}