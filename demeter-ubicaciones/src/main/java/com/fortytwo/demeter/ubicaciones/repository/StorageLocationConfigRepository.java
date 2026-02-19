package com.fortytwo.demeter.ubicaciones.repository;

import com.fortytwo.demeter.ubicaciones.model.StorageLocationConfig;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StorageLocationConfigRepository implements PanacheRepositoryBase<StorageLocationConfig, UUID> {

    /**
     * Find all active configs for a storage location.
     * Used by ML processing to know what products to expect at this location.
     */
    public List<StorageLocationConfig> findActiveByStorageLocation(UUID storageLocationId) {
        return find("storageLocation.id = ?1 AND active = true", storageLocationId).list();
    }

    /**
     * Find all configs (active and inactive) for a storage location.
     */
    public List<StorageLocationConfig> findByStorageLocation(UUID storageLocationId) {
        return find("storageLocation.id = ?1", storageLocationId).list();
    }

    /**
     * Find specific config by location, product, and optional packaging.
     */
    public Optional<StorageLocationConfig> findByLocationProductPackaging(
            UUID storageLocationId, UUID productId, UUID packagingCatalogId) {
        if (packagingCatalogId == null) {
            return find("storageLocation.id = ?1 AND product.id = ?2 AND packagingCatalog IS NULL",
                    storageLocationId, productId).firstResultOptional();
        }
        return find("storageLocation.id = ?1 AND product.id = ?2 AND packagingCatalog.id = ?3",
                storageLocationId, productId, packagingCatalogId).firstResultOptional();
    }

    /**
     * Find all configs for a specific product across all locations.
     */
    public List<StorageLocationConfig> findByProduct(UUID productId) {
        return find("product.id = ?1", productId).list();
    }
}
