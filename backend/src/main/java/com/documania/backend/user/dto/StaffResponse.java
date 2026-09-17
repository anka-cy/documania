package com.documania.backend.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffResponse(
    UUID publicId,
    String email,
    String firstName,
    String lastName,
    boolean emailVerified,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
