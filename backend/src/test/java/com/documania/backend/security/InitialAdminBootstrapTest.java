package com.documania.backend.security;

import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.role.RoleRepository;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InitialAdminBootstrapTest {

    private Environment environment;
    private UserAccountRepository userAccountRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private InitialAdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        userAccountRepository = mock(UserAccountRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        bootstrap = new InitialAdminBootstrap(
            environment,
            userAccountRepository,
            roleRepository,
            passwordEncoder
        );
    }

    @Test
    void shouldDoNothingWhenBootstrapConfigurationIsAbsent() {
        bootstrap.run(null);

        verifyNoInteractions(userAccountRepository, roleRepository, passwordEncoder);
    }

    @Test
    void shouldRejectIncompleteBootstrapConfiguration() {
        when(environment.getProperty("INITIAL_ADMIN_EMAIL"))
            .thenReturn("admin@documania.test");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        verifyNoInteractions(userAccountRepository, roleRepository, passwordEncoder);
    }

    @Test
    void shouldNotReplaceExistingAdministratorAccount() {
        configureAllProperties();
        // Le bootstrap normalise l'e-mail (trim + minuscules) avant la recherche.
        when(userAccountRepository.existsByEmailIgnoreCase("admin@documania.test"))
            .thenReturn(true);

        bootstrap.run(null);

        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    void shouldCreateVerifiedEnabledAdministrator() {
        configureAllProperties();
        Role adminRole = new Role(RoleName.ADMIN, "Administrateur");
        when(userAccountRepository.existsByEmailIgnoreCase("admin@documania.test"))
            .thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("MotDePasseTest123!"))
            .thenReturn("bcrypt-hash");

        bootstrap.run(null);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        UserAccount savedAccount = accountCaptor.getValue();

        assertEquals("admin@documania.test", savedAccount.getEmail());
        assertEquals("bcrypt-hash", savedAccount.getPasswordHash());
        assertEquals(RoleName.ADMIN, savedAccount.getRole().getName());
        assertEquals("Admin", savedAccount.getFirstName());
        assertEquals("Documania", savedAccount.getLastName());
        assertTrue(savedAccount.isEmailVerified());
        assertTrue(savedAccount.isEnabled());
    }

    private void configureAllProperties() {
        when(environment.getProperty("INITIAL_ADMIN_EMAIL"))
            .thenReturn("  ADMIN@documania.test  ");
        when(environment.getProperty("INITIAL_ADMIN_PASSWORD"))
            .thenReturn("MotDePasseTest123!");
        when(environment.getProperty("INITIAL_ADMIN_FIRST_NAME"))
            .thenReturn(" Admin ");
        when(environment.getProperty("INITIAL_ADMIN_LAST_NAME"))
            .thenReturn(" Documania ");
    }
}
