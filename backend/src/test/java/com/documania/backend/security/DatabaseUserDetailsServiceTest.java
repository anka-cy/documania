package com.documania.backend.security;

import com.documania.backend.role.Permission;
import com.documania.backend.role.PermissionCode;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseUserDetailsServiceTest {

    private UserAccountRepository userAccountRepository;
    private DatabaseUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userAccountRepository = mock(UserAccountRepository.class);
        userDetailsService = new DatabaseUserDetailsService(userAccountRepository);
    }

    @Test
    void shouldLoadRoleAndPermissionAuthorities() {
        Role staff = new Role(RoleName.STAFF, "Personnel");
        staff.addPermission(new Permission(PermissionCode.CLIENT_READ));
        UserAccount account = new UserAccount(
            "staff@documania.test",
            "password-hash",
            staff,
            "Test",
            "Staff"
        );
        account.markEmailVerified();
        when(userAccountRepository.findByEmailIgnoreCase("staff@documania.test"))
            .thenReturn(Optional.of(account));

        UserDetails result = userDetailsService.loadUserByUsername("staff@documania.test");

        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF")));
        assertTrue(result.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("CLIENT_READ")));
    }

    @Test
    void shouldDisableLoginWhenEmailIsNotVerified() {
        Role client = new Role(RoleName.CLIENT, "Client");
        UserAccount account = new UserAccount(
            "client@documania.test",
            "password-hash",
            client,
            "Test",
            "Client"
        );
        when(userAccountRepository.findByEmailIgnoreCase("client@documania.test"))
            .thenReturn(Optional.of(account));

        UserDetails result = userDetailsService.loadUserByUsername("client@documania.test");

        assertFalse(result.isEnabled());
    }

    @Test
    void shouldThrowGenericErrorWhenAccountDoesNotExist() {
        when(userAccountRepository.findByEmailIgnoreCase("missing@documania.test"))
            .thenReturn(Optional.empty());

        assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("missing@documania.test")
        );
    }
}
