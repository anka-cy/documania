package com.documania.backend.client;

import com.documania.backend.client.dto.ClientResponse;
import com.documania.backend.user.UserAccount;

public final class ClientMapper {

    private ClientMapper() {
    }

    public static ClientResponse toResponse(Client client) {
        UserAccount account = client.getUserAccount();
        return new ClientResponse(
            client.getPublicId(),
            account.getEmail(),
            account.getFirstName(),
            account.getLastName(),
            client.getCompanyName(),
            client.getPhone(),
            client.getAddress(),
            client.getSector(),
            client.isArchived(),
            account.isEmailVerified(),
            account.isEnabled(),
            client.getArchivedAt(),
            client.getCreatedAt(),
            client.getUpdatedAt()
        );
    }
}
