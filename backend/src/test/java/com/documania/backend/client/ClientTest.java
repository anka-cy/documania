package com.documania.backend.client;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTest {

    @Test
    void shouldGeneratePublicIdOnlyOnceBeforeInsert() {
        Client client = newClient();

        client.generatePublicId();
        UUID firstPublicId = client.getPublicId();
        client.generatePublicId();

        assertNotNull(firstPublicId);
        assertEquals(firstPublicId, client.getPublicId());
    }

    @Test
    void shouldArchiveOnceAndRestore() {
        Client client = newClient();

        client.archive();
        LocalDateTime firstArchivedAt = client.getArchivedAt();
        client.archive();

        assertTrue(client.isArchived());
        assertNotNull(firstArchivedAt);
        assertEquals(firstArchivedAt, client.getArchivedAt());

        client.restore();

        assertFalse(client.isArchived());
        assertNull(client.getArchivedAt());
    }

    @Test
    void shouldChangeEditableCompanyDetails() {
        Client client = newClient();

        client.changeDetails(
            "New Company",
            "+212600000001",
            "Rabat",
            "Technology"
        );

        assertEquals("New Company", client.getCompanyName());
        assertEquals("+212600000001", client.getPhone());
        assertEquals("Rabat", client.getAddress());
        assertEquals("Technology", client.getSector());
    }

    private Client newClient() {
        Role clientRole = new Role(RoleName.CLIENT, "Client");
        UserAccount userAccount = new UserAccount(
            "contact@company.test",
            "hash",
            clientRole,
            "Client",
            "Contact"
        );
        return new Client(
            userAccount,
            "Company",
            "+212600000000",
            "Casablanca",
            "Services"
        );
    }
}
