package com.documania.backend.security;

import com.documania.backend.order.CustomerOrderController;
import com.documania.backend.order.CustomerOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerOrderController.class)
@Import(SecurityConfig.class)
class JwtAuthenticationFilterSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CustomerOrderService orderService;

    @Test
    void shouldAuthenticateRequestWithValidToken() throws Exception {
        String token = jwtService.generateToken("client@test.local", List.of("ROLE_CLIENT"));
        when(orderService.listForCurrentClient("client@test.local")).thenReturn(List.of());

        mockMvc.perform(get("/api/client/orders")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRequestWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/client/orders")
                .header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/client/orders"))
            .andExpect(status().isUnauthorized());
    }
}