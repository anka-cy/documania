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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StaffService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AccountActivationService activationService;

    public StaffService(
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AuditService auditService,
        AccountActivationService activationService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.activationService = activationService;
    }

    @Transactional
    public UserAccount createStaff(CreateStaffRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessRuleException("Un compte utilise déjà cette adresse e-mail");
        }

        Role staffRole = roleRepository.findByName(RoleName.STAFF)
            .orElseThrow(() -> new ResourceNotFoundException("Rôle STAFF introuvable"));

        String unusableSecret = UUID.randomUUID().toString();
        String unusablePasswordHash = passwordEncoder.encode(unusableSecret);

        UserAccount staffAccount = new UserAccount(
            normalizedEmail,
            unusablePasswordHash,
            staffRole,
            request.firstName().trim(),
            request.lastName().trim()
        );
        staffAccount.disable();

        UserAccount savedAccount = userAccountRepository.save(staffAccount);
        auditService.record(
            AuditAction.STAFF_CREATED,
            "STAFF",
            savedAccount.getId(),
            savedAccount.getPublicId(),
            savedAccount.getEmail(),
            Map.of(),
            Map.of(
                "email", savedAccount.getEmail(),
                "firstName", savedAccount.getFirstName(),
                "lastName", savedAccount.getLastName(),
                "enabled", savedAccount.isEnabled(),
                "emailVerified", savedAccount.isEmailVerified()
            ),
            null
        );

        activationService.sendStaffInvitation(savedAccount);

        return savedAccount;
    }

    public List<UserAccount> listStaff() {
        return userAccountRepository
            .findAllByRole_NameOrderByCreatedAtDesc(RoleName.STAFF);
    }

    public UserAccount findStaff(UUID publicId) {
        return userAccountRepository.findByPublicIdAndRole_Name(publicId, RoleName.STAFF)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Compte staff introuvable : " + publicId
            ));
    }

    @Transactional
    public UserAccount changeStaffEnabled(
        UUID publicId,
        ChangeStaffEnabledRequest request
    ) {
        UserAccount staffAccount = findStaff(publicId);
        boolean previouslyEnabled = staffAccount.isEnabled();

        if (request.enabled()) {
            if (!staffAccount.isEmailVerified()) {
                throw new BusinessRuleException(
                    "Un compte staff non vérifié ne peut pas être activé"
                );
            }
            staffAccount.enable();
        } else {
            staffAccount.disable();
        }

        UserAccount savedAccount = userAccountRepository.save(staffAccount);
        auditService.record(
            request.enabled() ? AuditAction.STAFF_ENABLED : AuditAction.STAFF_DISABLED,
            "STAFF",
            savedAccount.getId(),
            savedAccount.getPublicId(),
            savedAccount.getEmail(),
            Map.of("enabled", previouslyEnabled),
            Map.of("enabled", savedAccount.isEnabled()),
            null
        );
        return savedAccount;
    }

    @Transactional
    public void deleteStaff(UUID publicId, String reason) {
        UserAccount staffAccount = findStaff(publicId);
        auditService.record(
            AuditAction.STAFF_DELETED,
            "STAFF",
            staffAccount.getId(),
            staffAccount.getPublicId(),
            staffAccount.getEmail(),
            Map.of(
                "enabled", staffAccount.isEnabled(),
                "emailVerified", staffAccount.isEmailVerified()
            ),
            Map.of(),
            reason.trim()
        );
        userAccountRepository.delete(staffAccount);
    }
}
