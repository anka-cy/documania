package com.documania.backend.user;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import org.junit.jupiter.api.Test;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserAccountTest {

    @Test
    void shouldGeneratePublicIdOnlyOnceBeforeInsert() {
        UserAccount account = new UserAccount(
            "staff@documania.test",
            "hash",
            new Role(RoleName.STAFF, "Personnel"),
            "Sara",
            "Amrani"
        );

        account.generatePublicId();
        UUID firstPublicId = account.getPublicId();
        account.generatePublicId();

        assertNotNull(firstPublicId);
        assertEquals(firstPublicId, account.getPublicId());
    }

    @Test
    void shouldLetHibernateGenerateCreationAndUpdateTimestamps() throws Exception {
        Field createdAt = UserAccount.class.getDeclaredField("createdAt");
        Field updatedAt = UserAccount.class.getDeclaredField("updatedAt");

        assertNotNull(createdAt.getAnnotation(CreationTimestamp.class));
        assertNotNull(updatedAt.getAnnotation(UpdateTimestamp.class));
    }
}
