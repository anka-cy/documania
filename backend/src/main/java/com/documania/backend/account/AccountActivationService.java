package com.documania.backend.account;

import com.documania.backend.account.dto.IssueActivationTokenResponse;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailService;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountActivationService {

    private static final Duration ACTIVATION_TOKEN_TTL = Duration.ofHours(24);

    private final AccountTokenService tokenService;
    private final ClientRepository clientRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AccountActivationService(AccountTokenService tokenService, ClientRepository clientRepository,
                                    UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
                                    EmailService emailService) {
        this.tokenService = tokenService;
        this.clientRepository = clientRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public IssueActivationTokenResponse issueForClient(UUID clientPublicId) {
        Client client = clientRepository.findByPublicIdAndArchived(clientPublicId, false)
            .orElseThrow(() -> new ResourceNotFoundException("Client actif introuvable : " + clientPublicId));
        UserAccount account = client.getUserAccount();
        if (account.isEmailVerified()) {
            throw new BusinessRuleException("Ce compte client est déjà activé");
        }
        return issuePasswordSetupToken(account);
    }

    @Transactional
    public IssueActivationTokenResponse sendStaffInvitation(UserAccount account) {
        if (account.isEmailVerified()) {
            throw new BusinessRuleException("Ce compte staff est déjà vérifié");
        }
        return issuePasswordSetupToken(account);
    }

    @Transactional
    public IssueActivationTokenResponse issueForStaff(UUID staffPublicId) {
        UserAccount account = userAccountRepository.findByPublicIdAndRole_Name(staffPublicId, RoleName.STAFF)
            .orElseThrow(() -> new ResourceNotFoundException("Compte staff introuvable : " + staffPublicId));
        return sendStaffInvitation(account);
    }

    private IssueActivationTokenResponse issuePasswordSetupToken(UserAccount account) {
        LocalDateTime expiresAt = tokenService.issue(
            account,
            AccountTokenType.PASSWORD_SETUP,
            ACTIVATION_TOKEN_TTL,
            (rawToken, expiry) -> emailService.buildAccountActivationMessage(account.getEmail(), rawToken, expiry)
        );
        return new IssueActivationTokenResponse(expiresAt);
    }

    @Transactional
    public void activate(String rawToken, String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new BusinessRuleException("Le mot de passe et sa confirmation ne correspondent pas");
        }
        AccountToken token = tokenService.consume(rawToken, AccountTokenType.PASSWORD_SETUP, "activation");
        LocalDateTime now = LocalDateTime.now();

        UserAccount account = token.getUserAccount();
        if (account.isEmailVerified()) {
            throw new BusinessRuleException("Ce compte est déjà activé");
        }
        if (account.getRole().getName() == RoleName.CLIENT
            && !clientRepository.existsByUserAccount_IdAndArchived(account.getId(), false)) {
            throw new BusinessRuleException("Ce compte client est archivé ou indisponible");
        }
        account.changePasswordHash(passwordEncoder.encode(password));
        account.markEmailVerified();
        account.enable();
        token.markUsed(now);
        userAccountRepository.save(account);
    }
}
