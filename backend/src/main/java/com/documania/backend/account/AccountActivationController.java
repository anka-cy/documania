package com.documania.backend.account;

import com.documania.backend.account.dto.ActivateAccountRequest;
import com.documania.backend.account.dto.IssueActivationTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AccountActivationController {

    private final AccountActivationService activationService;

    public AccountActivationController(AccountActivationService activationService) {
        this.activationService = activationService;
    }

    @PostMapping("/staff/clients/{id}/activation-token")
    @PreAuthorize("hasRole('ADMIN')")
    public IssueActivationTokenResponse issue(@PathVariable UUID id) {
        return activationService.issueForClient(id);
    }

    @PostMapping("/staff/accounts/{id}/activation-token")
    @PreAuthorize("hasRole('ADMIN')")
    public IssueActivationTokenResponse issueStaffInvitation(@PathVariable UUID id) {
        return activationService.issueForStaff(id);
    }

    @PostMapping("/public/account-activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@Valid @RequestBody ActivateAccountRequest request) {
        activationService.activate(request.token(), request.password(), request.passwordConfirmation());
    }
}
