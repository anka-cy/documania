package com.documania.backend.auth;

import com.documania.backend.auth.dto.LoginResponse;
import com.documania.backend.common.error.GlobalExceptionHandler;
import com.documania.backend.common.exception.LoginLockedException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnTokenOnSuccessfulLogin() throws Exception {
        when(authService.login(eq("client@test.local"), eq("correct-password"), anyString()))
            .thenReturn(new LoginResponse("jwt-token", "Bearer", "refresh-token", "client@test.local", "CLIENT"));

        mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "client@test.local",
                      "password": "correct-password"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.refreshToken").value((Object) null))
            .andExpect(jsonPath("$.email").value("client@test.local"))
            .andExpect(jsonPath("$.role").value("CLIENT"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("DOCUMANIA_REFRESH=refresh-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void shouldReturnValidationErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "not-an-email",
                      "password": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.email").exists())
            .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(anyString(), anyString(), anyString()))
            .thenThrow(new BadCredentialsException("bad credentials"));

        mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "client@test.local",
                      "password": "wrong-password"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void shouldReturnTooManyRequestsWhenAccountLocked() throws Exception {
        when(authService.login(anyString(), anyString(), anyString()))
            .thenThrow(new LoginLockedException("Trop de tentatives de connexion"));

        mockMvc.perform(post("/api/public/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "client@test.local",
                      "password": "correct-password"
                    }
                    """))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Trop de tentatives de connexion"));
    }

    @Test
    void shouldReturnNewTokensOnRefresh() throws Exception {
        when(authService.refresh("valid-refresh-token"))
            .thenReturn(new LoginResponse("new-jwt", "Bearer", "new-refresh-token", "client@test.local", "CLIENT"));

        mockMvc.perform(post("/api/public/auth/refresh")
                .cookie(new Cookie("DOCUMANIA_REFRESH", "valid-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("new-jwt"))
            .andExpect(jsonPath("$.refreshToken").value((Object) null))
            .andExpect(jsonPath("$.email").value("client@test.local"))
            .andExpect(jsonPath("$.role").value("CLIENT"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("DOCUMANIA_REFRESH=new-refresh-token")));
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshCookieMissing() throws Exception {
        mockMvc.perform(post("/api/public/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidRefreshToken() throws Exception {
        when(authService.refresh(anyString()))
            .thenThrow(new BadCredentialsException("bad refresh token"));

        mockMvc.perform(post("/api/public/auth/refresh")
                .cookie(new Cookie("DOCUMANIA_REFRESH", "invalid-refresh-token")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void shouldRevokeTokenOnLogout() throws Exception {
        mockMvc.perform(post("/api/public/auth/logout")
                .cookie(new Cookie("DOCUMANIA_REFRESH", "valid-refresh-token")))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("DOCUMANIA_REFRESH=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("valid-refresh-token", null);
    }

    @Test
    void shouldStillClearCookieWhenLogoutTokenMissing() throws Exception {
        mockMvc.perform(post("/api/public/auth/logout"))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("DOCUMANIA_REFRESH=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService, never()).logout(anyString(), anyString());
    }
}