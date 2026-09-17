package com.documania.backend.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateAccountRequest(
    @NotBlank(message = "Le jeton est obligatoire") String token,
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 12, max = 72, message = "Le mot de passe doit contenir entre 12 et 72 caractères")
    String password,
    @NotBlank(message = "La confirmation est obligatoire") String passwordConfirmation
) {
}
