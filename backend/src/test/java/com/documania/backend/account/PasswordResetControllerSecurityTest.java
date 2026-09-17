package com.documania.backend.account;

import com.documania.backend.security.RateLimitService;
import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@Import(SecurityConfig.class)
class PasswordResetControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void shouldAllowAnonymousResetRequestWithCsrf() throws Exception {
        mockMvc.perform(post("/api/public/password-reset/request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "client@test.local"
                    }
                    """))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldAllowAnonymousResetConfirmWithCsrf() throws Exception {
        mockMvc.perform(post("/api/public/password-reset/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "raw-token",
                      "password": "A-secure-password-123",
                      "passwordConfirmation": "A-secure-password-123"
                    }
                    """))
            .andExpect(status().isNoContent());
    }
}