package com.documania.backend.security;

import tools.jackson.databind.ObjectMapper;
import com.documania.backend.common.error.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public JwtService jwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        return new JwtService(secret, expirationMs);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
        JwtService jwtService,
        org.springframework.beans.factory.ObjectProvider<TokenBlacklistService> tokenBlacklistServiceProvider
    ) {
        return new JwtAuthenticationFilter(jwtService, tokenBlacklistServiceProvider.getIfAvailable());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/staff/accounts/**").hasRole("ADMIN")
                .requestMatchers("/api/staff/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/client/**").hasRole("CLIENT")
                .requestMatchers(
                    PathPatternRequestMatcher.pathPattern("/"),
                    PathPatternRequestMatcher.pathPattern("/index.html"),
                    PathPatternRequestMatcher.pathPattern("/assets/**"),
                    PathPatternRequestMatcher.pathPattern("/core/**"),
                    PathPatternRequestMatcher.pathPattern("/shared/**"),
                    PathPatternRequestMatcher.pathPattern("/layout/**"),
                    PathPatternRequestMatcher.pathPattern("/features/**")
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "Identifiants invalides", request.getRequestURI())
                )
                .accessDeniedHandler(deniedHandler())
            )
            .headers(headers -> headers
                .contentTypeOptions(contentTypeOptions -> {})
                .frameOptions(frameOptions -> frameOptions.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                .referrerPolicy(referrer -> referrer.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN
                ))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' https://cdn.jsdelivr.net; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                        "font-src 'self' data: https://cdn.jsdelivr.net; " +
                        "img-src 'self' data: blob:; " +
                        "connect-src 'self' https://cdn.jsdelivr.net; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'; " +
                        "object-src 'none'"
                    )
                )
                .permissionsPolicy(permissions -> permissions.policy(
                    "camera=(), microphone=(), geolocation=(), payment=(), usb=()"
                ))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AccessDeniedHandler deniedHandler() {
        return (request, response, accessDeniedException) -> writeError(
            response,
            HttpStatus.FORBIDDEN,
            "Vous n'avez pas la permission d'effectuer cette action.",
            request.getRequestURI()
        );
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, Map.of());
        objectMapper.writeValue(response.getWriter(), body);
    }
}