package com.documania.backend.subscription;

import com.documania.backend.subscription.dto.SubscriptionPageResponse;
import com.documania.backend.subscription.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.documania.backend.subscription.dto.CancelSubscriptionRequest;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api")
public class SubscriptionController {
    private final SubscriptionQueryService queryService;
    private final SubscriptionCommandService commandService;
    public SubscriptionController(SubscriptionQueryService queryService, SubscriptionCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping("/client/subscriptions")
    public List<SubscriptionResponse> listMine(Authentication authentication) {
        return queryService.listForCurrentClient(authentication.getName());
    }

    @GetMapping("/staff/subscriptions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SUBSCRIPTION_READ')")
    public SubscriptionPageResponse listAll(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) SubscriptionStatus status
    ) {
        return queryService.listAll(query, status, page, size);
    }

    @PatchMapping("/staff/subscriptions/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponse cancel(@PathVariable UUID id,
                                       @Valid @RequestBody CancelSubscriptionRequest request,
                                       Authentication authentication) {
        return commandService.cancel(authentication.getName(), id, request.reason());
    }
}
