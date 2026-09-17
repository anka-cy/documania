package com.documania.backend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final String senderAddress;
    private final String frontendBaseUrl;

    public EmailService(
        JavaMailSender mailSender,
        @Value("${app.mail.from}") String senderAddress,
        @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
        this.frontendBaseUrl = removeTrailingSlash(frontendBaseUrl);
    }

    public EmailMessage buildAccountActivationMessage(String recipient, String rawToken, LocalDateTime expiresAt) {
        String activationUrl = frontendBaseUrl + "/#/activate-account?token=" + rawToken;
        String body = """
            Bonjour,

            Votre compte Documania a été créé.
            Utilisez ce lien pour choisir votre mot de passe et activer votre compte :

            %s

            Ce lien expire le %s.
            Si vous n'attendiez pas ce message, vous pouvez l'ignorer.

            Documania
            """.formatted(activationUrl, expiresAt.format(DATE_FORMAT));
        return new EmailMessage(
            recipient,
            "Activez votre compte Documania",
            body
        );
    }

    public EmailMessage buildPasswordResetMessage(String recipient, String rawToken, LocalDateTime expiresAt) {
        String resetUrl = frontendBaseUrl + "/#/password-reset-confirm?token=" + rawToken;
        String body = """
            Bonjour,

            Une demande de réinitialisation de mot de passe a été faite pour votre compte Documania.
            Utilisez ce lien pour choisir un nouveau mot de passe :

            %s

            Ce lien expire le %s.
            Si vous n'êtes pas à l'origine de cette demande, vous pouvez l'ignorer.

            Documania
            """.formatted(resetUrl, expiresAt.format(DATE_FORMAT));
        return new EmailMessage(
            recipient,
            "Réinitialisez votre mot de passe Documania",
            body
        );
    }

    public EmailMessage buildEmailVerificationMessage(String recipient, String rawToken, LocalDateTime expiresAt) {
        String verificationUrl = frontendBaseUrl + "/#/verify-email?token=" + rawToken;
        String body = """
            Bonjour,

            Bienvenue sur Documania !
            Confirmez votre adresse e-mail pour activer votre compte client :

            %s

            Ce lien expire le %s.
            Si vous n'êtes pas à l'origine de cette inscription, vous pouvez l'ignorer.

            Documania
            """.formatted(verificationUrl, expiresAt.format(DATE_FORMAT));
        return new EmailMessage(
            recipient,
            "Confirmez votre adresse e-mail Documania",
            body
        );
    }

    public EmailMessage buildPasswordChangedMessage(String recipient) {
        String body = """
            Bonjour,

            Le mot de passe de votre compte Documania vient d'être modifié.
            Si vous n'êtes pas à l'origine de ce changement, contactez immédiatement un administrateur.

            Documania
            """;
        return new EmailMessage(recipient, "Votre mot de passe Documania a été modifié", body);
    }

    public EmailMessage buildOrderConfirmedMessage(
        String recipient,
        String orderNumber,
        String offerName,
        LocalDate startDate,
        LocalDate endDate
    ) {
        String body = """
            Bonjour,

            Votre commande %s pour l'offre « %s » a été confirmée.
            Votre abonnement est actif du %s au %s.

            Documania
            """.formatted(orderNumber, offerName, startDate.toString(), endDate.toString());
        return new EmailMessage(recipient, "Commande Documania confirmée", body);
    }

    public EmailMessage buildOrderRejectedMessage(String recipient, String orderNumber, String reason) {
        String body = """
            Bonjour,

            Votre commande %s a été rejetée.
            Motif : %s

            Documania
            """.formatted(orderNumber, reason);
        return new EmailMessage(recipient, "Commande Documania rejetée", body);
    }

    public EmailMessage buildSubscriptionActivatedMessage(
        String recipient,
        String companyName,
        String offerName,
        LocalDate startDate,
        LocalDate endDate
    ) {
        String body = """
            Bonjour,

            L'abonnement de %s à l'offre « %s » est actif du %s au %s.

            Documania
            """.formatted(companyName, offerName, startDate.toString(), endDate.toString());
        return new EmailMessage(recipient, "Votre abonnement Documania est actif", body);
    }

    public EmailMessage buildSubscriptionExpiryReminderMessage(
        String recipient,
        String companyName,
        LocalDate endDate
    ) {
        String body = """
            Bonjour,

            L'abonnement de %s arrivera à expiration le %s.
            Contactez-nous pour organiser son renouvellement.

            Documania
            """.formatted(companyName, endDate.toString());
        return new EmailMessage(recipient, "Votre abonnement Documania expire bientôt", body);
    }

    public EmailMessage buildTicketOpenedMessage(
        String recipient,
        String subject,
        String ticketReference
    ) {
        String body = """
            Bonjour,

            Votre demande « %s » (%s) a bien été enregistrée.
            Notre équipe va prendre en charge votre demande rapidement.

            Vous pouvez suivre l'avancement depuis votre espace client.

            Documania
            """.formatted(subject, ticketReference);
        return new EmailMessage(recipient, "Votre demande " + ticketReference + " a été créée", body);
    }

    public EmailMessage buildTicketReplyMessage(
        String recipient,
        String subject,
        String ticketReference
    ) {
        String body = """
            Bonjour,

            Un nouveau message a été publié sur votre demande « %s » (%s).

            Consultez votre espace client pour y répondre.

            Documania
            """.formatted(subject, ticketReference);
        return new EmailMessage(recipient, "Nouveau message sur " + ticketReference, body);
    }

    public EmailMessage buildTicketClosedMessage(
        String recipient,
        String subject,
        String ticketReference
    ) {
        String body = """
            Bonjour,

            Votre demande « %s » (%s) a été clôturée.

            Si le problème persiste, n'hésitez pas à ouvrir une nouvelle demande.

            Documania
            """.formatted(subject, ticketReference);
        return new EmailMessage(recipient, "Votre demande " + ticketReference + " est clôturée", body);
    }

    public void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public record EmailMessage(String recipient, String subject, String body) {    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
