package com.documania.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "L'e-mail est obligatoire")
    @Email(message = "L'e-mail doit être valide")
    String email,

    @NotBlank(message = "Le mot de passe est obligatoire")
    String password
) {
}