package com.documania.backend.user;

import com.documania.backend.common.error.GlobalExceptionHandler;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

class StaffControllerTest {

    private static final UUID STAFF_PUBLIC_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    private StaffService staffService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        staffService = mock(StaffService.class);
        StaffController staffController = new StaffController(staffService);
        mockMvc = MockMvcBuilders
            .standaloneSetup(staffController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldCreateStaffAndReturnCreatedStatus() throws Exception {
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
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "staff@documania.test",
                      "firstName": "Sara",
                      "lastName": "Amrani"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("staff@documania.test"))
            .andExpect(jsonPath("$.firstName").value("Sara"))
            .andExpect(jsonPath("$.lastName").value("Amrani"))
            .andExpect(jsonPath("$.emailVerified").value(false))
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(staffService).createStaff(any());
    }

    @Test
    void shouldReturnValidationErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/staff/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "invalid-email",
                      "firstName": "",
                      "lastName": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("Les données envoyées sont invalides"))
            .andExpect(jsonPath("$.fieldErrors.email").exists())
            .andExpect(jsonPath("$.fieldErrors.firstName").exists())
            .andExpect(jsonPath("$.fieldErrors.lastName").exists());
    }

    @Test
    void shouldReturnStaffList() throws Exception {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        when(staffService.listStaff()).thenReturn(List.of(account));

        mockMvc.perform(get("/api/staff/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("staff@documania.test"))
            .andExpect(jsonPath("$[0].firstName").value("Sara"));

        verify(staffService).listStaff();
    }

    @Test
    void shouldChangeStaffEnabledState() throws Exception {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        account.disable();
        when(staffService.changeStaffEnabled(
            org.mockito.ArgumentMatchers.eq(STAFF_PUBLIC_ID), any()
        )).thenReturn(account);

        mockMvc.perform(patch("/api/staff/accounts/" + STAFF_PUBLIC_ID + "/enabled")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void shouldRejectMissingEnabledValue() throws Exception {
        mockMvc.perform(patch("/api/staff/accounts/" + STAFF_PUBLIC_ID + "/enabled")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.enabled").exists());
    }

    @Test
    void shouldDeleteStaffAndReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/staff/accounts/" + STAFF_PUBLIC_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Employee left the company\"}"))
            .andExpect(status().isNoContent())
            .andExpect(jsonPath("$").doesNotExist());

        verify(staffService).deleteStaff(STAFF_PUBLIC_ID, "Employee left the company");
    }
}
