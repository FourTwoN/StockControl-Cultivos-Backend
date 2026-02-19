package com.fortytwo.demeter.ubicaciones.service;

import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.empaquetado.repository.PackagingCatalogRepository;
import com.fortytwo.demeter.productos.repository.ProductRepository;
import com.fortytwo.demeter.ubicaciones.model.StorageLocationConfig;
import com.fortytwo.demeter.ubicaciones.repository.StorageLocationConfigRepository;
import com.fortytwo.demeter.ubicaciones.repository.StorageLocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing storage location configurations.
 * Configurations define which products (and optional packaging) are tracked at each location.
 */
@ApplicationScoped
public class StorageLocationConfigService {

    private static final Logger log = Logger.getLogger(StorageLocationConfigService.class);

    @Inject
    StorageLocationConfigRepository configRepository;

    @Inject
    StorageLocationRepository storageLocationRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    PackagingCatalogRepository packagingCatalogRepository;

    /**
     * Get all active configurations for a storage location.
     * Used during ML processing to determine which products to track.
     */
    public List<StorageLocationConfig> getActiveConfigsByLocation(UUID storageLocationId) {
        return configRepository.findActiveByStorageLocation(storageLocationId);
    }

    /**
     * Find configuration by exact match of location + product + packaging.
     */
    public Optional<StorageLocationConfig> findByLocationProductPackaging(
            UUID storageLocationId, UUID productId, UUID packagingCatalogId) {
        return configRepository.findByLocationProductPackaging(storageLocationId, productId, packagingCatalogId);
    }

    /**
     * Check if a product is configured for tracking at a location.
     */
    public boolean isProductConfiguredAtLocation(UUID storageLocationId, UUID productId) {
        return configRepository.findActiveByStorageLocation(storageLocationId)
                .stream()
                .anyMatch(c -> c.getProduct().getId().equals(productId));
    }

    @Transactional
    public StorageLocationConfig create(UUID storageLocationId, UUID productId, UUID packagingCatalogId, String notes) {
        // Check for existing config
        Optional<StorageLocationConfig> existing = configRepository.findByLocationProductPackaging(
                storageLocationId, productId, packagingCatalogId);
        if (existing.isPresent()) {
            log.warnf("Config already exists for location=%s, product=%s, packaging=%s",
                    storageLocationId, productId, packagingCatalogId);
            return existing.get();
        }

        StorageLocationConfig config = new StorageLocationConfig();
        config.setStorageLocation(storageLocationRepository.findByIdOptional(storageLocationId)
                .orElseThrow(() -> new EntityNotFoundException("StorageLocation", storageLocationId)));
        config.setProduct(productRepository.findByIdOptional(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId)));

        if (packagingCatalogId != null) {
            config.setPackagingCatalog(packagingCatalogRepository.findByIdOptional(packagingCatalogId)
                    .orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", packagingCatalogId)));
        }

        config.setActive(true);
        config.setNotes(notes);

        configRepository.persist(config);
        log.infof("Created storage location config: location=%s, product=%s", storageLocationId, productId);

        return config;
    }

    @Transactional
    public void deactivate(UUID configId) {
        StorageLocationConfig config = configRepository.findByIdOptional(configId)
                .orElseThrow(() -> new EntityNotFoundException("StorageLocationConfig", configId));
        config.setActive(false);
        log.infof("Deactivated storage location config: %s", configId);
    }

    @Transactional
    public void activate(UUID configId) {
        StorageLocationConfig config = configRepository.findByIdOptional(configId)
                .orElseThrow(() -> new EntityNotFoundException("StorageLocationConfig", configId));
        config.setActive(true);
        log.infof("Activated storage location config: %s", configId);
    }

    @Transactional
    public void delete(UUID configId) {
        StorageLocationConfig config = configRepository.findByIdOptional(configId)
                .orElseThrow(() -> new EntityNotFoundException("StorageLocationConfig", configId));
        configRepository.delete(config);
        log.infof("Deleted storage location config: %s", configId);
    }
}
