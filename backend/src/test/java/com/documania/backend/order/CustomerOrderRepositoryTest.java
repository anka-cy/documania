package com.documania.backend.order;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerOrderRepositoryTest {

    @Test
    void shouldLockClientCancelLookupToPreventProcessingRace() throws Exception {
        Method method = CustomerOrderRepository.class.getMethod(
            "findByPublicIdAndClient_Id", UUID.class, Long.class);
        Lock lock = method.getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }
}
