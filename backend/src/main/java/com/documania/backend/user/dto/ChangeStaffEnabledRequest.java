package com.documania.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeStaffEnabledRequest(
    @NotNull(message = "L'état enabled est obligatoire")
    Boolean enabled
) {
}
