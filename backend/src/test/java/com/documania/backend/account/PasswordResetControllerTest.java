package com.documania.backend.account;

import com.documania.backend.common.error.GlobalExceptionHandler;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetControllerTest {

    private PasswordResetService passwordResetService;
    private RateLimitService rateLimitService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        passwordResetService = mock(PasswordResetService.class);
        rateLimitService = mock(RateLimitService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new PasswordResetController(passwordResetService, rateLimitService, 5, Duration.ofHours(1)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldQueueResetForValidEmail() throws Exception {
        mockMvc.perform(post("/api/public/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "client@test.local"
                    }
                    """))
            .andExpect(status().isNoContent());

        verify(passwordResetService).requestReset("client@test.local");
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/public/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "not-an-email"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void shouldConfirmResetWithValidPayload() throws Exception {
        mockMvc.perform(post("/api/public/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "raw-token",
                      "password": "A-secure-password-123",
                      "passwordConfirmation": "A-secure-password-123"
                    }
                    """))
            .andExpect(status().isNoContent());

        verify(passwordResetService).confirmReset("raw-token", "A-secure-password-123", "A-secure-password-123");
    }

    @Test
    void shouldRejectBlankToken() throws Exception {
        mockMvc.perform(post("/api/public/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "",
                      "password": "A-secure-password-123",
                      "passwordConfirmation": "A-secure-password-123"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.token").exists());
    }

    @Test
    void shouldRejectPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/public/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "raw-token",
                      "password": "short",
                      "passwordConfirmation": "short"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void shouldReturnConflictForInvalidResetToken() throws Exception {
        doThrow(new BusinessRuleException("Le jeton de réinitialisation est invalide"))
            .when(passwordResetService).confirmReset("raw-token", "A-secure-password-123", "A-secure-password-123");

        mockMvc.perform(post("/api/public/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "raw-token",
                      "password": "A-secure-password-123",
                      "passwordConfirmation": "A-secure-password-123"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Le jeton de réinitialisation est invalide"));
    }
}