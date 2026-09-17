package com.documania.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogServiceRepository extends JpaRepository<CatalogService, Long> {

    Optional<CatalogService> findByPublicIdAndArchived(UUID publicId, boolean archived);

    List<CatalogService> findAllByArchivedOrderByCreatedAtDesc(boolean archived);

    long countByArchived(boolean archived);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
