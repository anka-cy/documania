package com.documania.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
    @NotBlank(message = "L'e-mail est obligatoire")
    @Email(message = "L'e-mail doit être valide")
    @Size(max = 255, message = "L'e-mail ne peut pas dépasser 255 caractères")
    String email,

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    String firstName,

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String lastName
) {
}
