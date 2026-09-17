package com.documania.backend.account;

import com.documania.backend.client.Client;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.security.RateLimitService;
import com.documania.backend.security.SecurityConfig;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicRegistrationController.class)
@Import(SecurityConfig.class)
class PublicRegistrationControllerTest {

    private static final String VALID_REGISTRATION = """
        {
          "email":"newclient@test.local",
          "firstName":"Sara",
          "lastName":"Amrani",
          "companyName":"New Company",
          "phone":"+212600000000",
          "address":"Rabat",
          "sector":"Tech",
          "password":"A-strong-password-123",
          "passwordConfirmation":"A-strong-password-123"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicRegistrationService registrationService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void shouldAllowPublicRegistrationWithoutAuthentication() throws Exception {
        Client client = new Client(
            new UserAccount("newclient@test.local", "hash", new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani"),
            "New Company", null, null, null
        );
        when(registrationService.register(any())).thenReturn(client);

        mockMvc.perform(post("/api/public/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REGISTRATION))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.companyName").value("New Company"));
    }

    @Test
    void shouldRejectInvalidRegistrationWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/public/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"not-an-email",
                      "firstName":"Sara",
                      "lastName":"Amrani",
                      "companyName":"New Company",
                      "password":"short",
                      "passwordConfirmation":"short"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowEmailVerificationWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/public/account-verification")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"raw-token\"}"))
            .andExpect(status().isNoContent());
    }
}