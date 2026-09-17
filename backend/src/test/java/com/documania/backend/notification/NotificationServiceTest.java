package com.documania.backend.notification;

import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.notification.dto.NotificationPageResponse;
import com.documania.backend.notification.dto.NotificationResponse;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository repository;
    private NotificationStreamService streamService;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        streamService = mock(NotificationStreamService.class);
        service = new NotificationService(repository, streamService);
    }

    private UserAccount account(Long id) {
        UserAccount account = new UserAccount("client@test.local", "hash",
            new Role(RoleName.CLIENT, "Client"), "Sara", "Amrani");
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Notification notification(UUID publicId, boolean read) {
        Notification notification = new Notification(account(1L), NotificationType.ORDER_CONFIRMED,
            "Commande confirmée", "Message", "#/client/orders");
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "publicId", publicId);
        if (read) notification.markRead();
        return notification;
    }

    @Test
    void shouldCreateNotification() {
        when(repository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        Notification saved = service.notify(account(1L), NotificationType.ORDER_CONFIRMED,
            "Titre", "Message", "#/client/orders");

        assertNotNull(saved.getPublicId());
        verify(streamService).publish("user:1", "notification",
            java.util.Map.of("type", "ORDER_CONFIRMED"));
    }

    @Test
    void shouldNotifyAllRecipientsInOneBatch() {
        when(repository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        service.notifyAll(List.of(account(1L), account(2L), account(3L)),
            NotificationType.NEW_ORDER_PENDING, "Titre", "Message", "#/staff/orders");

        org.mockito.ArgumentCaptor<List<Notification>> captor =
            org.mockito.ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());
        verify(repository, never()).save(any(Notification.class));
        verify(streamService).publish("user:1", "notification", java.util.Map.of("type", "NEW_ORDER_PENDING"));
        verify(streamService).publish("user:2", "notification", java.util.Map.of("type", "NEW_ORDER_PENDING"));
        verify(streamService).publish("user:3", "notification", java.util.Map.of("type", "NEW_ORDER_PENDING"));
    }

    @Test
    void shouldSkipNotifyAllWhenNoRecipients() {
        service.notifyAll(List.of(), NotificationType.NEW_ORDER_PENDING, "Titre", "Message", "#/staff/orders");
        service.notifyAll(null, NotificationType.NEW_ORDER_PENDING, "Titre", "Message", "#/staff/orders");

        verify(repository, never()).saveAll(anyList());
        verify(repository, never()).save(any(Notification.class));
        verifyNoInteractions(streamService);
    }

    @Test
    void shouldListRecentForRecipient() {
        Notification n = notification(UUID.randomUUID(), false);
        when(repository.findTop10ByRecipient_IdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(n));

        List<NotificationResponse> result = service.listRecentForRecipient(1L);

        assertEquals(1, result.size());
        assertFalse(result.get(0).read());
    }

    @Test
    void shouldCountUnread() {
        when(repository.countByRecipient_IdAndReadAtIsNull(1L)).thenReturn(3L);

        assertEquals(3L, service.countUnread(1L));
    }

    @Test
    void shouldListNotificationsPaginatedForRecipient() {
        Notification n = notification(UUID.randomUUID(), false);
        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 25);
        when(repository.findByRecipient_IdOrderByCreatedAtDesc(1L, PageRequest.of(0, 20)))
            .thenReturn(page);

        NotificationPageResponse response = service.listForRecipient(1L, PageRequest.of(0, 20));

        assertEquals(1, response.items().size());
        assertFalse(response.items().get(0).read());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(25, response.totalElements());
        assertEquals(2, response.totalPages());
    }

    @Test
    void shouldMarkNotificationAsRead() {
        UUID publicId = UUID.randomUUID();
        Notification n = notification(publicId, false);
        when(repository.findByPublicIdAndRecipient_Id(publicId, 1L)).thenReturn(Optional.of(n));
        when(repository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        NotificationResponse response = service.markRead(1L, publicId);

        assertTrue(response.read());
        assertTrue(n.isRead());
    }

    @Test
    void shouldRejectMarkingUnknownNotificationAsRead() {
        UUID publicId = UUID.randomUUID();
        when(repository.findByPublicIdAndRecipient_Id(publicId, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markRead(1L, publicId));
    }

    @Test
    void shouldMarkAllReadForRecipient() {
        when(repository.markAllReadForRecipient(1L)).thenReturn(5);

        assertEquals(5, service.markAllRead(1L));
    }
}
