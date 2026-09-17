package com.documania.backend.user;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.dto.StaffResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StaffMapperTest {

    @Test
    void shouldMapStaffAccountToResponse() {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test",
            "password-hash",
            staffRole,
            "Sara",
            "Amrani"
        );
        account.markEmailVerified();
        account.generatePublicId();

        StaffResponse response = StaffMapper.toResponse(account);

        assertEquals("staff@documania.test", response.email());
        assertNotNull(response.publicId());
        assertEquals("Sara", response.firstName());
        assertEquals("Amrani", response.lastName());
        assertTrue(response.emailVerified());
        assertTrue(response.enabled());
        assertFalse(response.toString().contains("password-hash"));
    }
}
