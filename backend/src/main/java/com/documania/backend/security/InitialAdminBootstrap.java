package com.documania.backend.security;

import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.role.RoleRepository;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final String EMAIL_PROPERTY = "INITIAL_ADMIN_EMAIL";
    private static final String PASSWORD_PROPERTY = "INITIAL_ADMIN_PASSWORD";
    private static final String FIRST_NAME_PROPERTY = "INITIAL_ADMIN_FIRST_NAME";
    private static final String LAST_NAME_PROPERTY = "INITIAL_ADMIN_LAST_NAME";

    private final Environment environment;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminBootstrap(
        Environment environment,
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.environment = environment;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String email = environment.getProperty(EMAIL_PROPERTY);
        String password = environment.getProperty(PASSWORD_PROPERTY);
        String firstName = environment.getProperty(FIRST_NAME_PROPERTY);
        String lastName = environment.getProperty(LAST_NAME_PROPERTY);

        if (allBlank(email, password, firstName, lastName)) {
            return;
        }

        if (anyBlank(email, password, firstName, lastName)) {
            throw new IllegalStateException(
                "La configuration de l'administrateur initial est incomplète"
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
            .orElseThrow(() -> new ResourceNotFoundException("Rôle ADMIN introuvable"));

        UserAccount adminAccount = new UserAccount(
            normalizedEmail,
            passwordEncoder.encode(password),
            adminRole,
            firstName.trim(),
            lastName.trim()
        );
        adminAccount.markEmailVerified();

        userAccountRepository.save(adminAccount);
    }

    private boolean allBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private boolean anyBlank(String... values) {
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                return true;
            }
        }
        return false;
    }
}
