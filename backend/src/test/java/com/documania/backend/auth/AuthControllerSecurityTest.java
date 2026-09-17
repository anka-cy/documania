package com.documania.backend.auth;

import com.documania.backend.auth.dto.LoginResponse;
import com.documania.backend.security.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldAllowAnonymousLoginWithCsrf() throws Exception {
        when(authService.login(eq("client@test.local"), eq("correct-password"), anyString()))
            .thenReturn(new LoginResponse("jwt-token", "Bearer", "refresh-token", "client@test.local", "CLIENT"));

        mockMvc.perform(post("/api/public/auth/login")
                .with(csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "client@test.local",
                      "password": "correct-password"
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAnonymousRefreshWithCsrf() throws Exception {
        when(authService.refresh("valid-refresh-token"))
            .thenReturn(new LoginResponse("new-jwt", "Bearer", "new-refresh-token", "client@test.local", "CLIENT"));

        mockMvc.perform(post("/api/public/auth/refresh")
                .with(csrf())
                .cookie(new Cookie("DOCUMANIA_REFRESH", "valid-refresh-token")))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAnonymousLogoutWithCsrf() throws Exception {
        mockMvc.perform(post("/api/public/auth/logout")
                .with(csrf())
                .cookie(new Cookie("DOCUMANIA_REFRESH", "valid-refresh-token")))
            .andExpect(status().isNoContent());
    }
}