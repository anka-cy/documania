package com.documania.backend.ticket;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TicketRepositoryTest {

    @Test
    void shouldLockWriteLookupsToPreventRaces() throws Exception {
        assertWriteLock("findByPublicId", UUID.class);
        assertWriteLock("findByPublicIdAndClient_Id", UUID.class, Long.class);
    }

    @Test
    void shouldNotLockReadOnlyDetailLookups() throws Exception {
        // Les vues de détail tournent en transaction readOnly : un verrou
        // FOR UPDATE y est refusé par MySQL ("Cannot execute statement in a
        // READ ONLY transaction"). Elles doivent rester sans @Lock.
        assertNoLock("findForReadByPublicId", UUID.class);
        assertNoLock("findForReadByPublicIdAndClient_Id", UUID.class, Long.class);
    }

    private void assertWriteLock(String name, Class<?>... params) throws Exception {
        Method method = TicketRepository.class.getMethod(name, params);
        Lock lock = method.getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    private void assertNoLock(String name, Class<?>... params) throws Exception {
        Method method = TicketRepository.class.getMethod(name, params);
        assertNull(method.getAnnotation(Lock.class), name + " ne doit pas porter @Lock");
    }
}
