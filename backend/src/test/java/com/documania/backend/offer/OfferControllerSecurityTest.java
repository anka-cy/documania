package com.documania.backend.offer;

import com.documania.backend.catalog.CatalogService;
import com.documania.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfferController.class)
@Import(SecurityConfig.class)
class OfferControllerSecurityTest {

    private static final UUID SERVICE_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OFFER_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String VALID_REQUEST = """
        {
          "name":"Standard",
          "price":99.90,
          "durationMonths":12,
          "numberOfUsers":10,
          "commercialStartDate":"2026-01-01",
          "commercialEndDate":"2026-12-31"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfferManagementService offerManagement;

    @Test
    void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/staff/services/" + SERVICE_ID + "/offers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRejectClientFromStaffApi() throws Exception {
        mockMvc.perform(get("/api/staff/services/" + SERVICE_ID + "/offers"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffReadingWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/staff/services/" + SERVICE_ID + "/offers"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "OFFER_READ"})
    void shouldAllowStaffReadingWithPermission() throws Exception {
        when(offerManagement.listActiveOffers(SERVICE_ID)).thenReturn(List.of());
        mockMvc.perform(get("/api/staff/services/" + SERVICE_ID + "/offers"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "OFFER_CREATE"})
    void shouldAllowStaffCreatingWithPermission() throws Exception {
        when(offerManagement.createOffer(eq(SERVICE_ID), any())).thenReturn(offer());
        mockMvc.perform(post("/api/staff/services/" + SERVICE_ID + "/offers")
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffCreatingWithoutPermission() throws Exception {
        mockMvc.perform(post("/api/staff/services/" + SERVICE_ID + "/offers")
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "OFFER_UPDATE"})
    void shouldAllowStaffUpdatingWithPermission() throws Exception {
        when(offerManagement.updateOffer(eq(OFFER_ID), any())).thenReturn(offer());
        mockMvc.perform(put("/api/staff/offers/" + OFFER_ID)
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffUpdatingWithoutPermission() throws Exception {
        mockMvc.perform(put("/api/staff/offers/" + OFFER_ID)
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "OFFER_ARCHIVE"})
    void shouldAllowStaffArchivingWithPermission() throws Exception {
        when(offerManagement.archiveOffer(OFFER_ID)).thenReturn(offer());
        mockMvc.perform(patch("/api/staff/offers/" + OFFER_ID + "/archive").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffArchivingWithoutPermission() throws Exception {
        mockMvc.perform(patch("/api/staff/offers/" + OFFER_ID + "/archive").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "OFFER_RESTORE"})
    void shouldAllowStaffRestoringWithPermission() throws Exception {
        when(offerManagement.restoreOffer(OFFER_ID)).thenReturn(offer());
        mockMvc.perform(patch("/api/staff/offers/" + OFFER_ID + "/restore").with(csrf()))
            .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorDeletingArchivedOffer() throws Exception {
        mockMvc.perform(delete("/api/staff/offers/" + OFFER_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Offer permanently retired\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "OFFER_DELETE"})
    void shouldRejectStaffDeletingEvenWithDeleteAuthority() throws Exception {
        mockMvc.perform(delete("/api/staff/offers/" + OFFER_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Offer permanently retired\"}"))
            .andExpect(status().isForbidden());
    }

    private Offer offer() {
        return new Offer(
            new CatalogService("Cloud", null, null),
            "Standard", null, new BigDecimal("99.90"), 12, 10,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
    }
}
