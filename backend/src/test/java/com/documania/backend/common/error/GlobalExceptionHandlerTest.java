package com.documania.backend.common.error;

import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.LoginLockedException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @GetMapping("/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Ressource introuvable");
        }

        @GetMapping("/conflict")
        public void conflict() {
            throw new BusinessRuleException("Opération refusée");
        }

        @GetMapping("/locked")
        public void locked() {
            throw new LoginLockedException("Trop de tentatives de connexion");
        }

        @GetMapping("/denied")
        public void denied() {
            throw new AccessDeniedException("Denied");
        }

        @GetMapping("/error")
        public void error() {
            throw new IllegalStateException("something sensitive");
        }

        @PostMapping("/bad-json")
        public void badJson(@RequestBody Map<String, String> body) {
            // Lève HttpMessageNotReadableException si le corps n'est pas un JSON valide.
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnNotFoundJson() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Ressource introuvable"))
            .andExpect(jsonPath("$.path").value("/test/not-found"))
            .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void shouldReturnConflictJson() throws Exception {
        mockMvc.perform(get("/test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Opération refusée"));
    }

    @Test
    void shouldReturnTooManyRequestsJson() throws Exception {
        mockMvc.perform(get("/test/locked"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.error").value("Too Many Requests"))
            .andExpect(jsonPath("$.message").value("Trop de tentatives de connexion"));
    }

    @Test
    void shouldReturnForbiddenJson() throws Exception {
        mockMvc.perform(get("/test/denied"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message")
                .value("Vous n'avez pas la permission d'effectuer cette action."));
    }

    @Test
    void shouldReturnGeneric500WithoutLeakingInternals() throws Exception {
        mockMvc.perform(get("/test/error"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.message").value("Erreur interne du serveur. Réessayez plus tard."))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/test/bad-json")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not valid json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Format de requête invalide."));
    }
}