package com.documania.backend.client;

import com.documania.backend.common.error.GlobalExceptionHandler;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientProfileControllerTest {

    private ClientProfileService clientProfileService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        clientProfileService = mock(ClientProfileService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ClientProfileController(clientProfileService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        authentication = new UsernamePasswordAuthenticationToken(
            "client@test.local", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    private Client client() {
        UserAccount account = new UserAccount(
            "client@test.local", "current-hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"
        );
        account.markEmailVerified();
        return new Client(account, "Sara Corp", null, "Rabat", "Tech");
    }

    @Test
    void shouldReturnOwnProfile() throws Exception {
        when(clientProfileService.getProfile("client@test.local")).thenReturn(client());

        mockMvc.perform(get("/api/client/profile").principal(authentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("client@test.local"))
            .andExpect(jsonPath("$.firstName").value("Sara"))
            .andExpect(jsonPath("$.companyName").value("Sara Corp"));
    }

    @Test
    void shouldRejectProfileUpdateBecauseEndpointRemoved() throws Exception {
        mockMvc.perform(put("/api/client/profile")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Sara",
                      "lastName": "Amrani",
                      "companyName": "Sara Corp",
                      "phone": "",
                      "address": "Rabat",
                      "sector": "Tech"
                    }
                    """))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldChangePassword() throws Exception {
        mockMvc.perform(post("/api/client/change-password")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "current-password",
                      "newPassword": "A-new-secure-password",
                      "newPasswordConfirmation": "A-new-secure-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        verify(clientProfileService).changePassword(
            "client@test.local", "current-password", "A-new-secure-password", "A-new-secure-password");
    }

    @Test
    void shouldRejectShortNewPassword() throws Exception {
        mockMvc.perform(post("/api/client/change-password")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "current-password",
                      "newPassword": "short",
                      "newPasswordConfirmation": "short"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void shouldRejectWrongCurrentPasswordWithConflict() throws Exception {
        doThrow(new BusinessRuleException("Le mot de passe actuel est incorrect"))
            .when(clientProfileService).changePassword(
                "client@test.local", "wrong-password", "A-new-secure-password", "A-new-secure-password");

        mockMvc.perform(post("/api/client/change-password")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "wrong-password",
                      "newPassword": "A-new-secure-password",
                      "newPasswordConfirmation": "A-new-secure-password"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Le mot de passe actuel est incorrect"));
    }
}