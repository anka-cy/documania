package com.documania.backend.subscription;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionExpirationSchedulerTest {
    @Test void shouldDelegateExpirationUsingCurrentDate() {
        SubscriptionExpirationService service = mock(SubscriptionExpirationService.class);
        new SubscriptionExpirationScheduler(service).expireEndedSubscriptions();
        verify(service).expireEndedSubscriptions(any(LocalDate.class));
    }
}
