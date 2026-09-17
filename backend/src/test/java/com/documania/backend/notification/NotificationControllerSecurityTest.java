package com.documania.backend.notification;

import com.documania.backend.notification.dto.NotificationResponse;
import com.documania.backend.security.SecurityConfig;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationStreamService notificationStreamService;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private UserAccount account() {
        UserAccount account = new UserAccount("client@test.local", "hash",
            new com.documania.backend.role.Role(com.documania.backend.role.RoleName.CLIENT, "Client"),
            "Sara", "Amrani");
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", 42L);
        return account;
    }

    private NotificationResponse notification() {
        return new NotificationResponse(UUID.randomUUID(), NotificationType.ORDER_CONFIRMED,
            "Titre", "Message", "#/client/orders", false, LocalDateTime.now());
    }

    @Test
    void shouldRejectAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/client/notifications/recent"))
            .andExpect(status().isUnauthorized());
    }

        @Test
    @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToCountUnreadNotifications() throws Exception {
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local"))
            .thenReturn(Optional.of(account()));
        when(notificationService.countUnread(any())).thenReturn(2L);

        mockMvc.perform(get("/api/client/notifications/unread-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToReadRecentNotifications() throws Exception {
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local"))
            .thenReturn(Optional.of(account()));
        when(notificationService.listRecentForRecipient(any())).thenReturn(List.of(notification()));

        mockMvc.perform(get("/api/client/notifications/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].message").value("Message"));
    }

    @Test
    @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToMarkNotificationRead() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local"))
            .thenReturn(Optional.of(account()));
        when(notificationService.markRead(any(), any())).thenReturn(notification());

        mockMvc.perform(patch("/api/client/notifications/" + publicId + "/read").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAnonymousStreamAccess() throws Exception {
        mockMvc.perform(get("/api/client/notifications/stream"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldOpenClientNotificationStream() throws Exception {
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local"))
            .thenReturn(Optional.of(account()));
        when(notificationStreamService.subscribe("user:42"))
            .thenReturn(new org.springframework.web.servlet.mvc.method.annotation.SseEmitter());

        mockMvc.perform(get("/api/client/notifications/stream")
                .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffStreamWithoutDashboardRead() throws Exception {
        mockMvc.perform(get("/api/staff/notifications/stream"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToMarkAllNotificationsRead() throws Exception {
        when(userAccountRepository.findByEmailIgnoreCase("client@test.local"))
            .thenReturn(Optional.of(account()));

        mockMvc.perform(patch("/api/client/notifications/read-all").with(csrf()))
            .andExpect(status().isNoContent());
    }
}
