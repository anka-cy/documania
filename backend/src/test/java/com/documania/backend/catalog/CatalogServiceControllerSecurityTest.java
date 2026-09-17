package com.documania.backend.catalog;

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

@WebMvcTest(CatalogServiceController.class)
@Import(SecurityConfig.class)
class CatalogServiceControllerSecurityTest {

    private static final UUID PUBLIC_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String VALID_REQUEST = """
        {"name":"Cloud Backup","description":"Secure backups","category":"Cloud"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogServiceManagementService serviceManagement;

    @Test
    void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/staff/services"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRejectClientFromStaffApi() throws Exception {
        mockMvc.perform(get("/api/staff/services"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffReadingWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/staff/services"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "SERVICE_READ"})
    void shouldAllowStaffReadingWithPermission() throws Exception {
        when(serviceManagement.listActiveServices()).thenReturn(List.of());

        mockMvc.perform(get("/api/staff/services"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "SERVICE_CREATE"})
    void shouldAllowStaffCreatingWithPermission() throws Exception {
        when(serviceManagement.createService(any())).thenReturn(service());

        mockMvc.perform(post("/api/staff/services")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffCreatingWithoutPermission() throws Exception {
        mockMvc.perform(post("/api/staff/services")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "SERVICE_UPDATE"})
    void shouldAllowStaffUpdatingWithPermission() throws Exception {
        when(serviceManagement.updateService(eq(PUBLIC_ID), any())).thenReturn(service());

        mockMvc.perform(put("/api/staff/services/" + PUBLIC_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffUpdatingWithoutPermission() throws Exception {
        mockMvc.perform(put("/api/staff/services/" + PUBLIC_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "SERVICE_ARCHIVE"})
    void shouldAllowStaffArchivingWithPermission() throws Exception {
        when(serviceManagement.archiveService(PUBLIC_ID)).thenReturn(service());
        mockMvc.perform(patch("/api/staff/services/" + PUBLIC_ID + "/archive").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffArchivingWithoutPermission() throws Exception {
        mockMvc.perform(patch("/api/staff/services/" + PUBLIC_ID + "/archive").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "SERVICE_RESTORE"})
    void shouldAllowStaffRestoringWithPermission() throws Exception {
        when(serviceManagement.restoreService(PUBLIC_ID)).thenReturn(service());
        mockMvc.perform(patch("/api/staff/services/" + PUBLIC_ID + "/restore").with(csrf()))
            .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorDeletingArchivedService() throws Exception {
        mockMvc.perform(delete("/api/staff/services/" + PUBLIC_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Service permanently retired\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "SERVICE_DELETE"})
    void shouldRejectStaffDeletingEvenWithDeleteAuthority() throws Exception {
        mockMvc.perform(delete("/api/staff/services/" + PUBLIC_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Service permanently retired\"}"))
            .andExpect(status().isForbidden());
    }

    private CatalogService service() {
        return new CatalogService("Cloud Backup", "Secure backups", "Cloud");
    }
}
