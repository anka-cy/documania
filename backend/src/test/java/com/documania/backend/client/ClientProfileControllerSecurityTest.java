package com.documania.backend.client;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.security.SecurityConfig;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientProfileController.class)
@Import(SecurityConfig.class)
class ClientProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientProfileService clientProfileService;

    @Test
    void shouldRejectAnonymousProfileAccess() throws Exception {
        mockMvc.perform(get("/api/client/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToReadOwnProfile() throws Exception {
        when(clientProfileService.getProfile("client@test.local")).thenReturn(client());

        mockMvc.perform(get("/api/client/profile"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToChangeOwnPassword() throws Exception {
        mockMvc.perform(post("/api/client/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "current-password",
                      "newPassword": "A-new-secure-password",
                      "newPasswordConfirmation": "A-new-secure-password"
                    }
                    """))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "staff@test.local", roles = "STAFF")
    void shouldRejectStaffFromClientProfile() throws Exception {
        mockMvc.perform(get("/api/client/profile"))
            .andExpect(status().isForbidden());
    }

    private Client client() {
        UserAccount account = new UserAccount(
            "client@test.local", "current-hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        account.markEmailVerified();
        return new Client(account, "Sara Corp", null, null, null);
    }
}