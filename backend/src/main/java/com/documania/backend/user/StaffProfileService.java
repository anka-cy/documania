package com.documania.backend.user;

import com.documania.backend.account.AccountPasswordService;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.role.RoleName;
import com.documania.backend.user.dto.UpdateStaffRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StaffProfileService {

    private final UserAccountRepository userAccountRepository;
    private final AccountPasswordService accountPasswordService;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;

    public StaffProfileService(
        UserAccountRepository userAccountRepository,
        AccountPasswordService accountPasswordService,
        EmailService emailService,
        EmailOutboxRepository emailOutboxRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.accountPasswordService = accountPasswordService;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
    }

    public UserAccount getProfile(String email) {
        return findStaffByEmail(email);
    }

    @Transactional
    public UserAccount updateProfile(String email, UpdateStaffRequest request) {
        UserAccount account = findStaffByEmail(email);
        account.changeName(request.firstName().trim(), request.lastName().trim());
        return userAccountRepository.save(account);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword, String newPasswordConfirmation) {
        UserAccount account = accountPasswordService.changePassword(
            findStaffByEmail(email), currentPassword, newPassword, newPasswordConfirmation);

        EmailService.EmailMessage message =
            emailService.buildPasswordChangedMessage(account.getEmail());
        emailOutboxRepository.save(new EmailOutbox(message.recipient(), message.subject(), message.body()));
    }

    private UserAccount findStaffByEmail(String email) {
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("Profil staff introuvable"));
        RoleName role = account.getRole().getName();
        if (role != RoleName.STAFF && role != RoleName.ADMIN) {
            throw new ResourceNotFoundException("Profil staff introuvable");
        }
        return account;
    }
}