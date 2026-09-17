package com.documania.backend.account;

import com.documania.backend.auth.RefreshTokenRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cœur commun du changement de mot de passe (client et staff) : vérifie le
 * mot de passe actuel et la confirmation, puis applique le nouveau hash et
 * révoque les jetons de rafraîchissement. Chaque profil-service ajoute sa
 * propre notification.
 */
@Service
public class AccountPasswordService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountPasswordService(
        UserAccountRepository userAccountRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount changePassword(UserAccount account,
                                      String currentPassword,
                                      String newPassword,
                                      String newPasswordConfirmation) {
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BusinessRuleException("Le mot de passe actuel est incorrect");
        }
        if (!newPassword.equals(newPasswordConfirmation)) {
            throw new BusinessRuleException("Le mot de passe et sa confirmation ne correspondent pas");
        }

        account.changePasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenRepository.deleteByUserAccount_Id(account.getId());
        return userAccountRepository.save(account);
    }
}
