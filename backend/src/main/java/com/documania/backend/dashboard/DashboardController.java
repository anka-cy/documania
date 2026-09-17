package com.documania.backend.dashboard;

import com.documania.backend.dashboard.dto.DashboardSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_READ')")
    public DashboardSummaryResponse summary(Authentication authentication) {
        boolean includeAuditEvents = authentication != null && authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("AUDIT_READ"));
        return dashboardService.summary(includeAuditEvents);
    }
}