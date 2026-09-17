package com.documania.backend.account;

import com.documania.backend.account.dto.RegisterClientRequest;
import com.documania.backend.account.dto.VerifyEmailRequest;
import com.documania.backend.client.ClientMapper;
import com.documania.backend.client.dto.ClientResponse;
import com.documania.backend.common.exception.RateLimitExceededException;
import com.documania.backend.common.util.ClientIp;
import com.documania.backend.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/public")
public class PublicRegistrationController {

    private final PublicRegistrationService registrationService;
    private final RateLimitService rateLimitService;
    private final int registerMaxAttempts;
    private final Duration registerWindow;

    public PublicRegistrationController(
        PublicRegistrationService registrationService,
        RateLimitService rateLimitService,
        @Value("${app.security.rate-limit.register.max-attempts:20}") int registerMaxAttempts,
        @Value("${app.security.rate-limit.register.window:PT1H}") Duration registerWindow
    ) {
        this.registrationService = registrationService;
        this.rateLimitService = rateLimitService;
        this.registerMaxAttempts = registerMaxAttempts;
        this.registerWindow = registerWindow;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse register(
        @Valid @RequestBody RegisterClientRequest request,
        HttpServletRequest servletRequest
    ) {
        String ip = ClientIp.from(servletRequest);
        String key = "register:" + ip;
        rateLimitService.record(key, registerMaxAttempts, registerWindow);
        if (rateLimitService.isLimited(key, registerMaxAttempts, registerWindow)) {
            throw new RateLimitExceededException("Trop de tentatives d'inscription. Réessayez plus tard.");
        }
        return ClientMapper.toResponse(registrationService.register(request));
    }

    @PostMapping("/account-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        registrationService.verifyEmail(request.token());
    }
}