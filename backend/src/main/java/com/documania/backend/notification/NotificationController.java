package com.documania.backend.notification;

import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.notification.dto.NotificationPageResponse;
import com.documania.backend.notification.dto.NotificationResponse;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final UserAccountRepository userAccountRepository;

    public NotificationController(
        NotificationService notificationService,
        NotificationStreamService notificationStreamService,
        UserAccountRepository userAccountRepository
    ) {
        this.notificationService = notificationService;
        this.notificationStreamService = notificationStreamService;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Flux SSE temps réel des notifications du client courant.
     * L'événement « notification » signale au client de recharger son badge ;
     * aucun contenu métier ne transite (le détail se lit par l'API REST).
     */
    @GetMapping(value = "/client/notifications/stream", produces = "text/event-stream")
    public SseEmitter streamForClient(Authentication authentication) {
        return notificationStreamService.subscribe(
            NotificationStreamService.userChannel(currentUserId(authentication)));
    }

    @GetMapping(value = "/staff/notifications/stream", produces = "text/event-stream")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_READ')")
    public SseEmitter streamForStaff(Authentication authentication) {
        return notificationStreamService.subscribe(
            NotificationStreamService.userChannel(currentUserId(authentication)));
    }

    @GetMapping("/client/notifications/unread-count")
    public Map<String, Long> unreadCountForClient(Authentication authentication) {
        return Map.of("count", notificationService.countUnread(currentUserId(authentication)));
    }

    @GetMapping("/staff/notifications/unread-count")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_READ')")
    public Map<String, Long> unreadCountForStaff(Authentication authentication) {
        return Map.of("count", notificationService.countUnread(currentUserId(authentication)));
    }

    @GetMapping("/client/notifications/recent")
    public List<NotificationResponse> recentForClient(Authentication authentication) {
        return notificationService.listRecentForRecipient(currentUserId(authentication));
    }

    @GetMapping("/staff/notifications/recent")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_READ')")
    public List<NotificationResponse> recentForStaff(Authentication authentication) {
        return notificationService.listRecentForRecipient(currentUserId(authentication));
    }

    @GetMapping("/client/notifications")
    public NotificationPageResponse listForClient(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication
    ) {
        return notificationService.listForRecipient(currentUserId(authentication),
            PageRequest.of(page, size));
    }

    @GetMapping("/staff/notifications")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_READ')")
    public NotificationPageResponse listForStaff(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication
    ) {
        return notificationService.listForRecipient(currentUserId(authentication),
            PageRequest.of(page, size));
    }

    @PatchMapping("/client/notifications/{id}/read")
    public NotificationResponse markReadClient(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        return notificationService.markRead(currentUserId(authentication), id);
    }

    @PatchMapping("/staff/notifications/{id}/read")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_READ')")
    public NotificationResponse markReadStaff(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        return notificationService.markRead(currentUserId(authentication), id);
    }

    @PatchMapping("/client/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllReadClient(Authentication authentication) {
        notificationService.markAllRead(currentUserId(authentication));
    }

    @PatchMapping("/staff/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('DASHBOARD_READ')")
    public void markAllReadStaff(Authentication authentication) {
        notificationService.markAllRead(currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        return userAccountRepository.findByEmailIgnoreCase(authentication.getName())
            .map(UserAccount::getId)
            .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + authentication.getName()));
    }
}