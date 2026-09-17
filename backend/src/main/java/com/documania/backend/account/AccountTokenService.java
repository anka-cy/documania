package com.documania.backend.account;

import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.security.TokenHash;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.BiFunction;

/**
 * Cœur commun des trois flux à jeton (activation, vérification e-mail,
 * réinitialisation de mot de passe) : invalide les jetons précédents non
 * utilisés, émet un nouveau jeton haché et place l'e-mail dans l'outbox.
 * Chaque flux ajoute ses propres règles métier avant/après.
 */
@Service
public class AccountTokenService {

    private final AccountTokenRepository tokenRepository;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;

    public AccountTokenService(
        AccountTokenRepository tokenRepository,
        EmailService emailService,
        EmailOutboxRepository emailOutboxRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
    }

    /**
     * Invalide les jetons précédents du même type puis émet un nouveau jeton
     * et son e-mail. Le {@code messageBuilder} reçoit le jeton brut (à placer
     * dans le lien) et la date d'expiration pour construire l'e-mail.
     */
    @Transactional
    public LocalDateTime issue(UserAccount account,
                               AccountTokenType type,
                               Duration ttl,
                               BiFunction<String, LocalDateTime, EmailService.EmailMessage> messageBuilder) {
        LocalDateTime now = LocalDateTime.now();
        for (AccountToken previous : tokenRepository.findAllByUserAccount_IdAndTypeAndUsedAtIsNull(
            account.getId(), type
        )) {
            previous.markUsed(now);
        }
        String rawToken = TokenHash.generateRaw();
        LocalDateTime expiresAt = now.plus(ttl);
        tokenRepository.save(new AccountToken(account, TokenHash.hash(rawToken), type, expiresAt));
        EmailService.EmailMessage message = messageBuilder.apply(rawToken, expiresAt);
        emailOutboxRepository.save(new EmailOutbox(message.recipient(), message.subject(), message.body()));
        return expiresAt;
    }

    /** Retrouve un jeton valide par sa valeur brute, ou lève une erreur métier. */
    @Transactional(readOnly = true)
    public AccountToken consume(String rawToken, AccountTokenType type, String tokenLabel) {
        AccountToken token = tokenRepository
            .findByTokenHashAndType(TokenHash.hash(rawToken), type)
            .orElseThrow(() -> new BusinessRuleException("Le jeton de " + tokenLabel + " est invalide"));
        if (!token.isUsableAt(LocalDateTime.now())) {
            throw new BusinessRuleException("Le jeton de " + tokenLabel + " est expiré ou déjà utilisé");
        }
        return token;
    }
}
