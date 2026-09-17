package com.documania.backend.account;

import com.documania.backend.common.util.TextUtils;
import com.documania.backend.account.dto.RegisterClientRequest;
import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailService;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.role.RoleRepository;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PublicRegistrationService {

    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);

    private final AccountTokenService tokenService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PublicRegistrationService(
        AccountTokenService tokenService,
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        ClientRepository clientRepository,
        PasswordEncoder passwordEncoder,
        EmailService emailService
    ) {
        this.tokenService = tokenService;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public Client register(RegisterClientRequest request) {
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new BusinessRuleException("Le mot de passe et sa confirmation ne correspondent pas");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Un compte utilise déjà cette adresse e-mail");
        }

        Role clientRole = roleRepository.findByName(RoleName.CLIENT)
            .orElseThrow(() -> new ResourceNotFoundException("Rôle CLIENT introuvable"));

        UserAccount account = new UserAccount(
            email,
            passwordEncoder.encode(request.password()),
            clientRole,
            sanitize(request.firstName()),
            sanitize(request.lastName())
        );
        account.disable();
        UserAccount savedAccount = userAccountRepository.save(account);

        Client client = new Client(
            savedAccount,
            sanitize(request.companyName()),
            TextUtils.optionalText(sanitize(request.phone())),
            TextUtils.optionalText(sanitize(request.address())),
            TextUtils.optionalText(sanitize(request.sector()))
        );
        Client savedClient = clientRepository.save(client);

        tokenService.issue(
            savedAccount,
            AccountTokenType.EMAIL_VERIFICATION,
            VERIFICATION_TOKEN_TTL,
            (rawToken, expiry) -> emailService.buildEmailVerificationMessage(savedAccount.getEmail(), rawToken, expiry)
        );

        return savedClient;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AccountToken token = tokenService.consume(rawToken, AccountTokenType.EMAIL_VERIFICATION, "vérification");
        LocalDateTime now = LocalDateTime.now();
        UserAccount account = token.getUserAccount();
        if (account.isEmailVerified()) {
            throw new BusinessRuleException("Cette adresse e-mail est déjà vérifiée");
        }
        if (!clientRepository.existsByUserAccount_IdAndArchived(account.getId(), false)) {
            throw new BusinessRuleException("Ce compte client est archivé ou indisponible");
        }

        account.markEmailVerified();
        account.enable();
        token.markUsed(now);
        userAccountRepository.save(account);
    }

    /**
     * Nettoie un texte saisi par l'utilisateur : retire les balises et les
     * caractères de contrôle, puis limite les longueurs. Défense en profondeur
     * en complément de l'échappement systématique à l'affichage.
     */
    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("<[^>]*>", " ")
            .replaceAll("[\\x00-\\x1F\\x7F]", "")
            .replaceAll("\\s{2,}", " ")
            .trim();
        int maxLength = 255;
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

}