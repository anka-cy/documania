package com.documania.backend.user;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.security.SecurityConfig;
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

import java.util.UUID;

@WebMvcTest(StaffController.class)
@Import(SecurityConfig.class)
class StaffControllerSecurityTest {

    private static final UUID STAFF_PUBLIC_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String VALID_REQUEST = """
        {
          "email": "staff@documania.test",
          "firstName": "Sara",
          "lastName": "Amrani"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffService staffService;

    @Test
    void shouldReturnUnauthorizedForAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/staff/accounts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldReturnForbiddenForStaff() throws Exception {
        mockMvc.perform(post("/api/staff/accounts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorWithCsrfToken() throws Exception {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test",
            "password-hash",
            staffRole,
            "Sara",
            "Amrani"
        );
        account.disable();
        when(staffService.createStaff(any())).thenReturn(account);

        mockMvc.perform(post("/api/staff/accounts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffReadingStaffAccounts() throws Exception {
        mockMvc.perform(get("/api/staff/accounts"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToReadStaffAccounts() throws Exception {
        when(staffService.listStaff()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/staff/accounts"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffUpdatingStaffAccount() throws Exception {
        mockMvc.perform(put("/api/staff/accounts/" + STAFF_PUBLIC_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Sara","lastName":"Amrani"}
                    """))
            .andExpect(status().isForbidden());
    }

        @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffChangingEnabledState() throws Exception {
        mockMvc.perform(patch("/api/staff/accounts/" + STAFF_PUBLIC_ID + "/enabled")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToChangeEnabledState() throws Exception {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        account.disable();
        when(staffService.changeStaffEnabled(
            org.mockito.ArgumentMatchers.eq(STAFF_PUBLIC_ID), any()
        )).thenReturn(account);

        mockMvc.perform(patch("/api/staff/accounts/" + STAFF_PUBLIC_ID + "/enabled")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void shouldRejectStaffDeletingStaffAccount() throws Exception {
        mockMvc.perform(delete("/api/staff/accounts/" + STAFF_PUBLIC_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Employee left the company\"}"))
            .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdministratorToDeleteStaffAccount() throws Exception {
        mockMvc.perform(delete("/api/staff/accounts/" + STAFF_PUBLIC_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Employee left the company\"}"))
            .andExpect(status().isNoContent());
    }
}
