package com.documania.backend.client;

import com.documania.backend.account.AccountPasswordService;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.email.EmailOutbox;
import com.documania.backend.email.EmailOutboxRepository;
import com.documania.backend.email.EmailService;
import com.documania.backend.notification.NotificationService;
import com.documania.backend.notification.NotificationType;
import com.documania.backend.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientProfileService {

    private final ClientRepository clientRepository;
    private final AccountPasswordService accountPasswordService;
    private final EmailService emailService;
    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationService notificationService;

    public ClientProfileService(
        ClientRepository clientRepository,
        AccountPasswordService accountPasswordService,
        EmailService emailService,
        EmailOutboxRepository emailOutboxRepository,
        NotificationService notificationService
    ) {
        this.clientRepository = clientRepository;
        this.accountPasswordService = accountPasswordService;
        this.emailService = emailService;
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationService = notificationService;
    }

    public Client getProfile(String email) {
        return findActiveByEmail(email);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword, String newPasswordConfirmation) {
        Client client = findActiveByEmail(email);
        UserAccount account = accountPasswordService.changePassword(
            client.getUserAccount(), currentPassword, newPassword, newPasswordConfirmation);

        EmailService.EmailMessage message =
            emailService.buildPasswordChangedMessage(account.getEmail());
        emailOutboxRepository.save(new EmailOutbox(message.recipient(), message.subject(), message.body()));

        notificationService.notify(account, NotificationType.PASSWORD_CHANGED, "Mot de passe modifié",
            "Le mot de passe de votre compte Documania a été modifié.", "#/client/profile");
    }

    private Client findActiveByEmail(String email) {
        return clientRepository.findByUserAccount_EmailIgnoreCaseAndArchived(email, false)
            .orElseThrow(() -> new ResourceNotFoundException("Profil client introuvable"));
    }
}
