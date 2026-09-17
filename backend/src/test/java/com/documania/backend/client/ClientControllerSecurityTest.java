package com.documania.backend.client;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.security.SecurityConfig;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import(SecurityConfig.class)
class ClientControllerSecurityTest {

    private static final String VALID_REQUEST = """
        {
          "email":"contact@company.test",
          "firstName":"Sara",
          "lastName":"Amrani",
          "companyName":"Example Company"
        }
        """;

    private static final String VALID_UPDATE_REQUEST = """
        {
          "firstName":"Sarah",
          "lastName":"Amrani",
          "companyName":"Updated Company",
          "phone":"+212600000001"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/staff/clients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffWithoutClientCreatePermission() throws Exception {
        mockMvc.perform(post("/api/staff/clients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_CREATE"})
    void shouldAllowStaffWithClientCreatePermission() throws Exception {
        when(clientService.createClient(any())).thenReturn(client());

        mockMvc.perform(post("/api/staff/clients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministrator() throws Exception {
        when(clientService.createClient(any())).thenReturn(client());

        mockMvc.perform(post("/api/staff/clients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isCreated());
    }


    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffReadingClientsWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/staff/clients"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_READ"})
    void shouldAllowStaffWithClientReadPermission() throws Exception {
        when(clientService.listActiveClients()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/staff/clients"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffReadingArchivedClientsWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/staff/clients/archived"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_READ"})
    void shouldAllowStaffWithClientReadPermissionToListArchivedClients() throws Exception {
        when(clientService.listArchivedClients()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/staff/clients/archived"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToListArchivedClients() throws Exception {
        when(clientService.listArchivedClients()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/staff/clients/archived"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToReadClientByUuid() throws Exception {
        java.util.UUID publicId =
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(clientService.findClientById(publicId)).thenReturn(client());

        mockMvc.perform(get("/api/staff/clients/" + publicId))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRejectClientFromStaffClientManagementApi() throws Exception {
        mockMvc.perform(get("/api/staff/clients"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffUpdatingClientWithoutPermission() throws Exception {
        mockMvc.perform(put("/api/staff/clients/11111111-1111-1111-1111-111111111111")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_REQUEST))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_UPDATE"})
    void shouldAllowStaffWithClientUpdatePermission() throws Exception {
        java.util.UUID publicId =
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(clientService.updateClient(org.mockito.ArgumentMatchers.eq(publicId), any()))
            .thenReturn(client());

        mockMvc.perform(put("/api/staff/clients/" + publicId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_REQUEST))
            .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffArchivingWithoutPermission() throws Exception {
        mockMvc.perform(patch(
                "/api/staff/clients/11111111-1111-1111-1111-111111111111/archive"
            ).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_ARCHIVE"})
    void shouldAllowStaffWithArchivePermission() throws Exception {
        java.util.UUID publicId =
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        Client client = client();
        client.archive();
        when(clientService.archiveClient(publicId)).thenReturn(client);

        mockMvc.perform(patch("/api/staff/clients/" + publicId + "/archive").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_RESTORE"})
    void shouldAllowStaffWithRestorePermission() throws Exception {
        java.util.UUID publicId =
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(clientService.restoreClient(publicId)).thenReturn(client());

        mockMvc.perform(patch("/api/staff/clients/" + publicId + "/restore").with(csrf()))
            .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(authorities = {"ROLE_STAFF", "CLIENT_DELETE"})
    void shouldRejectStaffDeletingClientEvenWithDeleteAuthority() throws Exception {
        mockMvc.perform(delete(
                "/api/staff/clients/11111111-1111-1111-1111-111111111111"
            ).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Company account permanently closed\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToDeleteArchivedClient() throws Exception {
        java.util.UUID publicId =
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(delete("/api/staff/clients/" + publicId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Company account permanently closed\"}"))
            .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(clientService)
            .deleteArchivedClient(publicId, "Company account permanently closed");
    }


    private Client client() {
        UserAccount account = new UserAccount(
            "contact@company.test",
            "hash",
            new Role(RoleName.CLIENT, "Client"),
            "Sara",
            "Amrani"
        );
        account.disable();
        return new Client(account, "Example Company", null, null, null);
    }
}
