package com.documania.backend.user;

import com.documania.backend.account.AccountActivationService;
import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.role.Role;
import com.documania.backend.role.RoleName;
import com.documania.backend.role.RoleRepository;
import com.documania.backend.user.dto.CreateStaffRequest;
import com.documania.backend.user.dto.ChangeStaffEnabledRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StaffServiceTest {

    private static final UUID STAFF_PUBLIC_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    private UserAccountRepository userAccountRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private AuditService auditService;
    private AccountActivationService activationService;
    private StaffService staffService;

    @BeforeEach
    void setUp() {
        userAccountRepository = mock(UserAccountRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditService = mock(AuditService.class);
        activationService = mock(AccountActivationService.class);
        staffService = new StaffService(
            userAccountRepository,
            roleRepository,
            passwordEncoder,
            auditService,
            activationService
        );
    }

    @Test
    void shouldCreateDisabledUnverifiedStaffAccount() {
        CreateStaffRequest request = new CreateStaffRequest(
            "  STAFF@documania.test  ",
            " Sara ",
            " Amrani "
        );
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        when(userAccountRepository.existsByEmailIgnoreCase("staff@documania.test"))
            .thenReturn(false);
        when(roleRepository.findByName(RoleName.STAFF)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode(anyString())).thenReturn("unusable-password-hash");
        when(userAccountRepository.save(org.mockito.ArgumentMatchers.any(UserAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount result = staffService.createStaff(request);

        assertEquals("staff@documania.test", result.getEmail());
        assertEquals("Sara", result.getFirstName());
        assertEquals("Amrani", result.getLastName());
        assertEquals(RoleName.STAFF, result.getRole().getName());
        assertEquals("unusable-password-hash", result.getPasswordHash());
        assertFalse(result.isEmailVerified());
        assertFalse(result.isEnabled());
        verify(passwordEncoder).encode(anyString());
        verify(userAccountRepository).save(result);
        verify(activationService).sendStaffInvitation(result);
        verify(auditService).record(
            AuditAction.STAFF_CREATED,
            "STAFF",
            null,
            null,
            "staff@documania.test",
            java.util.Map.of(),
            java.util.Map.of(
                "email", "staff@documania.test",
                "firstName", "Sara",
                "lastName", "Amrani",
                "enabled", false,
                "emailVerified", false
            ),
            null
        );
    }

    @Test
    void shouldRejectDuplicateEmailBeforeCreatingAccount() {
        CreateStaffRequest request = new CreateStaffRequest(
            "staff@documania.test",
            "Sara",
            "Amrani"
        );
        when(userAccountRepository.existsByEmailIgnoreCase("staff@documania.test"))
            .thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> staffService.createStaff(request));

        verify(userAccountRepository, never())
            .save(org.mockito.ArgumentMatchers.any(UserAccount.class));
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    void shouldFailWhenStaffRoleIsMissing() {
        CreateStaffRequest request = new CreateStaffRequest(
            "staff@documania.test",
            "Sara",
            "Amrani"
        );
        when(userAccountRepository.existsByEmailIgnoreCase("staff@documania.test"))
            .thenReturn(false);
        when(roleRepository.findByName(RoleName.STAFF)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> staffService.createStaff(request));

        verifyNoInteractions(passwordEncoder);
        verify(userAccountRepository, never())
            .save(org.mockito.ArgumentMatchers.any(UserAccount.class));
    }

    @Test
    void shouldListOnlyStaffAccountsNewestFirst() {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        List<UserAccount> accounts = List.of(
            new UserAccount("second@documania.test", "hash", staffRole, "Second", "Staff"),
            new UserAccount("first@documania.test", "hash", staffRole, "First", "Staff")
        );
        when(userAccountRepository.findAllByRole_NameOrderByCreatedAtDesc(RoleName.STAFF))
            .thenReturn(accounts);

        List<UserAccount> result = staffService.listStaff();

        assertEquals(accounts, result);
        verify(userAccountRepository)
            .findAllByRole_NameOrderByCreatedAtDesc(RoleName.STAFF);
    }

    @Test
    void shouldFindStaffByIdAndRole() {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        when(userAccountRepository.findByPublicIdAndRole_Name(STAFF_PUBLIC_ID, RoleName.STAFF))
            .thenReturn(Optional.of(account));

        UserAccount result = staffService.findStaff(STAFF_PUBLIC_ID);

        assertEquals(account, result);
    }

    @Test
    void shouldReportMissingStaffAccount() {
        when(userAccountRepository.findByPublicIdAndRole_Name(STAFF_PUBLIC_ID, RoleName.STAFF))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> staffService.findStaff(STAFF_PUBLIC_ID));
    }

    @Test
    void shouldDisableStaffAccount() {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        account.markEmailVerified();
        when(userAccountRepository.findByPublicIdAndRole_Name(STAFF_PUBLIC_ID, RoleName.STAFF))
            .thenReturn(Optional.of(account));
        when(userAccountRepository.save(account)).thenReturn(account);

        UserAccount result = staffService.changeStaffEnabled(
            STAFF_PUBLIC_ID,
            new ChangeStaffEnabledRequest(false)
        );

        assertFalse(result.isEnabled());
        verify(userAccountRepository).save(account);
        verify(auditService).record(
            AuditAction.STAFF_DISABLED,
            "STAFF",
            null,
            null,
            "staff@documania.test",
            java.util.Map.of("enabled", true),
            java.util.Map.of("enabled", false),
            null
        );
    }

    @Test
    void shouldEnableVerifiedStaffAccount() {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        account.markEmailVerified();
        account.disable();
        when(userAccountRepository.findByPublicIdAndRole_Name(STAFF_PUBLIC_ID, RoleName.STAFF))
            .thenReturn(Optional.of(account));
        when(userAccountRepository.save(account)).thenReturn(account);

        UserAccount result = staffService.changeStaffEnabled(
            STAFF_PUBLIC_ID,
            new ChangeStaffEnabledRequest(true)
        );

        assertTrue(result.isEnabled());
        verify(auditService).record(
            AuditAction.STAFF_ENABLED,
            "STAFF",
            null,
            null,
            "staff@documania.test",
            java.util.Map.of("enabled", false),
            java.util.Map.of("enabled", true),
            null
        );
    }

    @Test
    void shouldRejectEnablingUnverifiedStaffAccount() {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        account.disable();
        when(userAccountRepository.findByPublicIdAndRole_Name(STAFF_PUBLIC_ID, RoleName.STAFF))
            .thenReturn(Optional.of(account));

        assertThrows(
            BusinessRuleException.class,
            () -> staffService.changeStaffEnabled(
                STAFF_PUBLIC_ID,
                new ChangeStaffEnabledRequest(true)
            )
        );

        verify(userAccountRepository, never()).save(account);
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldDeleteStaffAccount() {
        Role staffRole = new Role(RoleName.STAFF, "Personnel");
        UserAccount account = new UserAccount(
            "staff@documania.test", "hash", staffRole, "Sara", "Amrani"
        );
        when(userAccountRepository.findByPublicIdAndRole_Name(STAFF_PUBLIC_ID, RoleName.STAFF))
            .thenReturn(Optional.of(account));

        staffService.deleteStaff(STAFF_PUBLIC_ID, "Employee left the company");

        verify(auditService).record(
            AuditAction.STAFF_DELETED,
            "STAFF",
            null,
            null,
            "staff@documania.test",
            java.util.Map.of("enabled", true, "emailVerified", false),
            java.util.Map.of(),
            "Employee left the company"
        );
        verify(userAccountRepository).delete(account);
    }

    @Test
    void shouldNotDeleteMissingOrNonStaffAccount() {
        when(userAccountRepository.findByPublicIdAndRole_Name(STAFF_PUBLIC_ID, RoleName.STAFF))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            staffService.deleteStaff(STAFF_PUBLIC_ID, "Employee left the company")
        );

        verify(userAccountRepository, never())
            .delete(org.mockito.ArgumentMatchers.any(UserAccount.class));
    }
}
