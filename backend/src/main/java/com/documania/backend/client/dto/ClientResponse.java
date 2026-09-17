package com.documania.backend.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientResponse(
    UUID publicId,
    String email,
    String firstName,
    String lastName,
    String companyName,
    String phone,
    String address,
    String sector,
    boolean archived,
    boolean emailVerified,
    boolean enabled,
    LocalDateTime archivedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
