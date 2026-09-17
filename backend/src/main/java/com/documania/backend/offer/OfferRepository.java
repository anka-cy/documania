package com.documania.backend.offer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    @EntityGraph(attributePaths = "catalogService")
    Optional<Offer> findByPublicIdAndArchived(UUID publicId, boolean archived);

    @EntityGraph(attributePaths = "catalogService")
    List<Offer> findAllByCatalogService_IdAndArchivedOrderByCreatedAtDesc(
        Long serviceId,
        boolean archived
    );

    @EntityGraph(attributePaths = "catalogService")

    boolean existsByCatalogService_IdAndName(Long serviceId, String name);

    boolean existsByCatalogService_IdAndNameAndIdNot(Long serviceId, String name, Long id);

    long countByArchived(boolean archived);
}
