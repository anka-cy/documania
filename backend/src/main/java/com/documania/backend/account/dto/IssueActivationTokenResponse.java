package com.documania.backend.account.dto;

import java.time.LocalDateTime;

public record IssueActivationTokenResponse(LocalDateTime expiresAt) {
}
