package com.documania.backend.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, Long> {

    @EntityGraph(attributePaths = "userAccount")
    Optional<Client> findByPublicIdAndArchived(UUID publicId, boolean archived);

    @EntityGraph(attributePaths = "userAccount")
    Optional<Client> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = "userAccount")
    List<Client> findAllByArchivedOrderByCreatedAtDesc(boolean archived);

    long countByArchived(boolean archived);


    boolean existsByUserAccount_IdAndArchived(Long userId, boolean archived);

    @EntityGraph(attributePaths = "userAccount")
    Optional<Client> findByUserAccount_EmailIgnoreCaseAndArchived(String email, boolean archived);
}
