package com.documania.backend.account.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
    @NotBlank(message = "Le jeton est obligatoire")
    String token
) {
}