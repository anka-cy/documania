package com.documania.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeStaffPasswordRequest(
    @NotBlank(message = "Le mot de passe actuel est obligatoire")
    String currentPassword,

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 12, max = 72, message = "Le nouveau mot de passe doit contenir entre 12 et 72 caractères")
    String newPassword,

    @NotBlank(message = "La confirmation est obligatoire")
    String newPasswordConfirmation
) {
}