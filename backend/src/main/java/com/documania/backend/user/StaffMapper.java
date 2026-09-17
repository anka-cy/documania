package com.documania.backend.user;

import com.documania.backend.user.dto.StaffResponse;

public final class StaffMapper {

    private StaffMapper() {
    }

    public static StaffResponse toResponse(UserAccount account) {
        return new StaffResponse(
            account.getPublicId(),
            account.getEmail(),
            account.getFirstName(),
            account.getLastName(),
            account.isEmailVerified(),
            account.isEnabled(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
