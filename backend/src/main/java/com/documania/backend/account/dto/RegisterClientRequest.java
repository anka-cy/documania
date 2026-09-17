package com.documania.backend.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterClientRequest(
    @NotBlank(message = "L'e-mail est obligatoire")
    @Email(message = "L'e-mail doit être valide")
    @Size(max = 255, message = "L'e-mail ne peut pas dépasser 255 caractères")
    String email,

    @NotBlank(message = "Le prénom du contact est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    String firstName,

    @NotBlank(message = "Le nom du contact est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String lastName,

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Size(max = 150, message = "Le nom de l'entreprise ne peut pas dépasser 150 caractères")
    String companyName,

    @Size(max = 30, message = "Le téléphone ne peut pas dépasser 30 caractères")
    String phone,

    @Size(max = 1000, message = "L'adresse ne peut pas dépasser 1000 caractères")
    String address,

    @Size(max = 100, message = "Le secteur ne peut pas dépasser 100 caractères")
    String sector,

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 12, max = 72, message = "Le mot de passe doit contenir entre 12 et 72 caractères")
    String password,

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    String passwordConfirmation
) {
}