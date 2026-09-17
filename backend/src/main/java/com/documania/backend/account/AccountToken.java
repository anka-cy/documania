package com.documania.backend.account;

import com.documania.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_tokens")
public class AccountToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountTokenType type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AccountToken() {
    }

    public AccountToken(UserAccount userAccount, String tokenHash, AccountTokenType type, LocalDateTime expiresAt) {
        this.userAccount = userAccount;
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    public UserAccount getUserAccount() { return userAccount; }
    public String getTokenHash() { return tokenHash; }
    public AccountTokenType getType() { return type; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public boolean isUsableAt(LocalDateTime time) { return usedAt == null && expiresAt.isAfter(time); }
    public void markUsed(LocalDateTime time) { this.usedAt = time; }
}
