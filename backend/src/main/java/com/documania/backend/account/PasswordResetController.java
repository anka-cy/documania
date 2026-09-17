package com.documania.backend.account;

import com.documania.backend.account.dto.PasswordResetRequest;
import com.documania.backend.account.dto.ResetPasswordRequest;
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
import java.util.Locale;

@RestController
@RequestMapping("/api/public/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RateLimitService rateLimitService;
    private final int resetMaxAttempts;
    private final Duration resetWindow;

    public PasswordResetController(
        PasswordResetService passwordResetService,
        RateLimitService rateLimitService,
        @Value("${app.security.rate-limit.reset.max-attempts:5}") int resetMaxAttempts,
        @Value("${app.security.rate-limit.reset.window:PT1H}") Duration resetWindow
    ) {
        this.passwordResetService = passwordResetService;
        this.rateLimitService = rateLimitService;
        this.resetMaxAttempts = resetMaxAttempts;
        this.resetWindow = resetWindow;
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void request(
        @Valid @RequestBody PasswordResetRequest request,
        HttpServletRequest servletRequest
    ) {
        String key = "reset-request:" + ClientIp.from(servletRequest) + ":" + request.email().trim().toLowerCase(Locale.ROOT);
        rateLimitService.record(key, resetMaxAttempts, resetWindow);
        if (rateLimitService.isLimited(key, resetMaxAttempts, resetWindow)) {
            throw new RateLimitExceededException("Trop de demandes de réinitialisation. Réessayez plus tard.");
        }
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(
        @Valid @RequestBody ResetPasswordRequest request,
        HttpServletRequest servletRequest
    ) {
        String key = "reset-confirm:" + ClientIp.from(servletRequest) + ":" + request.token();
        rateLimitService.record(key, resetMaxAttempts, resetWindow);
        if (rateLimitService.isLimited(key, resetMaxAttempts, resetWindow)) {
            throw new RateLimitExceededException("Trop de tentatives de réinitialisation. Réessayez plus tard.");
        }
        passwordResetService.confirmReset(request.token(), request.password(), request.passwordConfirmation());
    }
}