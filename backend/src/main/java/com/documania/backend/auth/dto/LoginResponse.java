package com.documania.backend.auth.dto;

public record LoginResponse(
    String token,
    String tokenType,
    String refreshToken,
    String email,
    String role
) {
}