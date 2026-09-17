package com.documania.backend.client;

import com.documania.backend.client.dto.ChangePasswordRequest;
import com.documania.backend.client.dto.ClientResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client")
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    public ClientProfileController(ClientProfileService clientProfileService) {
        this.clientProfileService = clientProfileService;
    }

    @GetMapping("/profile")
    public ClientResponse getProfile(Authentication authentication) {
        return ClientMapper.toResponse(clientProfileService.getProfile(authentication.getName()));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
        Authentication authentication,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        clientProfileService.changePassword(
            authentication.getName(),
            request.currentPassword(),
            request.newPassword(),
            request.newPasswordConfirmation()
        );
    }
}
