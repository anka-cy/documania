package com.documania.backend.auth;

import com.documania.backend.auth.dto.LoginRequest;
import com.documania.backend.auth.dto.LoginResponse;
import com.documania.backend.common.util.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/public/auth")
public class AuthController {

    static final String REFRESH_COOKIE_NAME = "DOCUMANIA_REFRESH";

    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(7);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        LoginResponse response = authService.login(
            request.email(),
            request.password(),
            ClientIp.from(servletRequest)
        );
        setRefreshCookie(servletRequest, servletResponse, response.refreshToken());
        return sanitized(response);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
        @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Le jeton de rafraîchissement est invalide");
        }
        LoginResponse response = authService.refresh(rawRefreshToken);
        setRefreshCookie(servletRequest, servletResponse, response.refreshToken());
        return sanitized(response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            authService.logout(rawRefreshToken, authHeader);
        }
        clearRefreshCookie(servletRequest, servletResponse);
    }

    /** Cache le jeton de rafraîchissement : il ne transite que dans le cookie HttpOnly. */
    private LoginResponse sanitized(LoginResponse response) {
        return new LoginResponse(response.token(), response.tokenType(), null, response.email(), response.role());
    }

    private void setRefreshCookie(
        HttpServletRequest request,
        HttpServletResponse response,
        String rawToken
    ) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
            .httpOnly(true)
            .secure(request.isSecure())
            .sameSite("Lax")
            .path("/api")
            .maxAge(REFRESH_COOKIE_MAX_AGE)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(request.isSecure())
            .sameSite("Lax")
            .path("/api")
            .maxAge(Duration.ZERO)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}