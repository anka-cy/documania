package com.documania.backend.account;

import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountActivationController.class)
@Import(SecurityConfig.class)
class AccountActivationControllerSecurityTest {

    private static final String CLIENT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ACTIVATION = """
        {"token":"token","password":"A-secure-password-123","passwordConfirmation":"A-secure-password-123"}
        """;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccountActivationService activationService;

    @Test
    void shouldAllowAnonymousActivation() throws Exception {
        mockMvc.perform(post("/api/public/account-activation")
                .contentType(MediaType.APPLICATION_JSON).content(ACTIVATION))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffIssuingToken() throws Exception {
        mockMvc.perform(post("/api/staff/clients/" + CLIENT_ID + "/activation-token").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminIssuingToken() throws Exception {
        mockMvc.perform(post("/api/staff/clients/" + CLIENT_ID + "/activation-token").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffIssuingStaffInvitation() throws Exception {
        mockMvc.perform(post("/api/staff/accounts/" + CLIENT_ID + "/activation-token").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminIssuingStaffInvitation() throws Exception {
        mockMvc.perform(post("/api/staff/accounts/" + CLIENT_ID + "/activation-token").with(csrf()))
            .andExpect(status().isOk());
    }
}
