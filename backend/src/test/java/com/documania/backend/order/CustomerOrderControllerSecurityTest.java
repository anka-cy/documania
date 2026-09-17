package com.documania.backend.order;

import com.documania.backend.order.dto.OrderPageResponse;
import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerOrderController.class)
@Import(SecurityConfig.class)
class CustomerOrderControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CustomerOrderService orderService;

    @Test void shouldRejectAnonymousClientOrderAccess() throws Exception {
        mockMvc.perform(get("/api/client/orders")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToReadOwnOrders() throws Exception {
        when(orderService.listForCurrentClient("client@test.local")).thenReturn(List.of());
        mockMvc.perform(get("/api/client/orders")).andExpect(status().isOk());
    }

    @Test @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToCreateOrderWithCsrf() throws Exception {
        UUID offerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        CustomerOrder order = mock(CustomerOrder.class);
        com.documania.backend.offer.Offer offer = mock(com.documania.backend.offer.Offer.class);
        when(order.getOffer()).thenReturn(offer);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING);
        when(orderService.createForCurrentClient("client@test.local", offerId)).thenReturn(order);
        mockMvc.perform(post("/api/client/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"offerId\":\"11111111-1111-1111-1111-111111111111\"}"))
            .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "STAFF")
    void shouldRejectStaffWithoutOrderReadPermission() throws Exception {
        mockMvc.perform(get("/api/staff/orders")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(authorities = {"ROLE_STAFF", "ORDER_READ"})
    void shouldAllowStaffWithOrderReadPermission() throws Exception {
        when(orderService.listAll(isNull(), isNull(), eq(0), eq(20)))
            .thenReturn(new OrderPageResponse(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/staff/orders")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToReadAllOrders() throws Exception {
        when(orderService.listAll(isNull(), isNull(), eq(0), eq(20)))
            .thenReturn(new OrderPageResponse(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/staff/orders")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldValidatePaginationBoundsOnStaffOrderList() throws Exception {
        mockMvc.perform(get("/api/staff/orders").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/orders").param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/staff/orders").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username = "client@test.local", roles = "CLIENT")
    void shouldAllowClientToCancelOwnOrderWithCsrf() throws Exception {
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        CustomerOrder order = mock(CustomerOrder.class);
        com.documania.backend.offer.Offer offer = mock(com.documania.backend.offer.Offer.class);
        when(order.getOffer()).thenReturn(offer);
        when(order.getStatus()).thenReturn(OrderStatus.CANCELLED);
        when(orderService.cancelForCurrentClient("client@test.local", orderId)).thenReturn(order);

        mockMvc.perform(patch("/api/client/orders/" + orderId + "/cancel").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldRejectAdminFromClientCancellationEndpoint() throws Exception {
        mockMvc.perform(patch("/api/client/orders/22222222-2222-2222-2222-222222222222/cancel").with(csrf()))
            .andExpect(status().isForbidden());
    }


    @Test @WithMockUser(username = "admin@test.local", roles = "ADMIN")
    void shouldAllowAdminToRejectPendingOrder() throws Exception {
        UUID orderId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        CustomerOrder order = mock(CustomerOrder.class);
        com.documania.backend.offer.Offer offer = mock(com.documania.backend.offer.Offer.class);
        when(order.getOffer()).thenReturn(offer);
        when(order.getStatus()).thenReturn(OrderStatus.REJECTED);
        when(orderService.reject("admin@test.local", orderId, "Dossier incomplet")).thenReturn(order);

        mockMvc.perform(patch("/api/staff/orders/" + orderId + "/reject").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Dossier incomplet\"}"))
            .andExpect(status().isOk());
    }

    @Test @WithMockUser(authorities = {"ROLE_STAFF", "ORDER_REJECT"})
    void shouldRejectStaffEvenWithOrderRejectAuthority() throws Exception {
        mockMvc.perform(patch("/api/staff/orders/33333333-3333-3333-3333-333333333333/reject").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Dossier incomplet\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldValidateRequiredRejectionReason() throws Exception {
        mockMvc.perform(patch("/api/staff/orders/33333333-3333-3333-3333-333333333333/reject").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username = "admin@test.local", roles = "ADMIN")
    void shouldAllowAdminToConfirmPendingOrder() throws Exception {
        UUID orderId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        CustomerOrder order = mock(CustomerOrder.class);
        com.documania.backend.offer.Offer offer = mock(com.documania.backend.offer.Offer.class);
        when(order.getOffer()).thenReturn(offer);
        when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);
        com.documania.backend.subscription.Subscription subscription =
            mock(com.documania.backend.subscription.Subscription.class);
        com.documania.backend.subscription.SubscriptionPeriod period =
            mock(com.documania.backend.subscription.SubscriptionPeriod.class);
        when(subscription.getClient()).thenReturn(mock(com.documania.backend.client.Client.class));
        when(subscription.getOffer()).thenReturn(offer);
        when(subscription.getCustomerOrder()).thenReturn(order);
        when(orderService.confirm("admin@test.local", orderId))
            .thenReturn(new CustomerOrderService.ConfirmedOrder(order, subscription, period));

        mockMvc.perform(patch("/api/staff/orders/" + orderId + "/confirm").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test @WithMockUser(authorities = {"ROLE_STAFF", "ORDER_CONFIRM"})
    void shouldRejectStaffEvenWithOrderConfirmAuthority() throws Exception {
        mockMvc.perform(patch("/api/staff/orders/44444444-4444-4444-4444-444444444444/confirm").with(csrf()))
            .andExpect(status().isForbidden());
    }

     @Test @WithMockUser(username = "admin@test.local", roles = "ADMIN")
     void shouldAllowAdminToCreateOrderForClient() throws Exception {
         UUID clientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
         UUID offerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
         CustomerOrder order = mock(CustomerOrder.class);
         com.documania.backend.offer.Offer offer = mock(com.documania.backend.offer.Offer.class);
         when(order.getOffer()).thenReturn(offer);
         com.documania.backend.subscription.Subscription subscription =
             mock(com.documania.backend.subscription.Subscription.class);
         com.documania.backend.subscription.SubscriptionPeriod period =
             mock(com.documania.backend.subscription.SubscriptionPeriod.class);
         when(subscription.getClient()).thenReturn(mock(com.documania.backend.client.Client.class));
         when(subscription.getOffer()).thenReturn(offer);
         when(subscription.getCustomerOrder()).thenReturn(order);
         when(orderService.createAndConfirmForClient("admin@test.local", clientId, offerId))
             .thenReturn(new CustomerOrderService.ConfirmedOrder(order, subscription, period));

         mockMvc.perform(post("/api/staff/orders").with(csrf())
                 .contentType(MediaType.APPLICATION_JSON)
                 .content("{\"clientId\":\"" + clientId + "\",\"offerId\":\"" + offerId + "\"}"))
             .andExpect(status().isCreated());
     }

    @Test @WithMockUser(roles = "STAFF")
    void shouldRejectStaffCreatingOrder() throws Exception {
        mockMvc.perform(post("/api/staff/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientId\":\"11111111-1111-1111-1111-111111111111\",\"offerId\":\"22222222-2222-2222-2222-222222222222\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void shouldValidateRequiredOrderCreationIdentifiers() throws Exception {
        mockMvc.perform(post("/api/staff/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }
}
