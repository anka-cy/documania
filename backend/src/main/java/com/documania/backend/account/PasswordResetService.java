package com.documania.backend.account;

import com.documania.backend.auth.RefreshTokenRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final AccountTokenService tokenService;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;

    public PasswordResetService(
        AccountTokenService tokenService,
        UserAccountRepository userAccountRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        EmailOutboxRepository emailOutboxRepository
    ) {
        this.tokenService = tokenService;
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
    }

    @Transactional
    public void requestReset(String email) {
        userAccountRepository.findByEmailIgnoreCase(email).ifPresent(account -> {
            tokenService.issue(
                account,
                AccountTokenType.PASSWORD_RESET,
                RESET_TOKEN_TTL,
                (rawToken, expiry) -> emailService.buildPasswordResetMessage(account.getEmail(), rawToken, expiry)
            );
        });
    }

    @Transactional
    public void confirmReset(String rawToken, String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new BusinessRuleException("Le mot de passe et sa confirmation ne correspondent pas");
        }
        AccountToken token = tokenService.consume(rawToken, AccountTokenType.PASSWORD_RESET, "réinitialisation");
        UserAccount account = token.getUserAccount();
        if (!account.isEnabled()) {
            throw new BusinessRuleException("Ce compte est désactivé");
        }

        account.changePasswordHash(passwordEncoder.encode(password));
        token.markUsed(LocalDateTime.now());
        refreshTokenRepository.deleteByUserAccount_Id(account.getId());
        userAccountRepository.save(account);

        EmailService.EmailMessage message =
            emailService.buildPasswordChangedMessage(account.getEmail());
        emailOutboxRepository.save(new EmailOutbox(message.recipient(), message.subject(), message.body()));
    }
}
