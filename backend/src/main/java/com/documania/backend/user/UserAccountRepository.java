package com.documania.backend.user;

import com.documania.backend.role.RoleName;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findAllByRole_NameOrderByCreatedAtDesc(RoleName roleName);

    /** Destinataires éligibles aux notifications staff : rôles donnés, compte actif et vérifié. */
    List<UserAccount> findAllByRole_NameInAndEnabledTrueAndEmailVerifiedTrue(Collection<RoleName> roleNames);

    Optional<UserAccount> findByPublicIdAndRole_Name(UUID publicId, RoleName roleName);
}
