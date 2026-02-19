package com.fortytwo.demeter.ubicaciones.model;

import com.fortytwo.demeter.common.model.BaseEntity;
import com.fortytwo.demeter.empaquetado.model.PackagingCatalog;
import com.fortytwo.demeter.productos.model.Product;
import jakarta.persistence.*;

/**
 * Configuration binding a storage location to a specific product (and optionally packaging).
 * Defines what can be stored in a location and enables ML processing to know
 * which product/packaging combination to expect at each location.
 */
@Entity
@Table(
    name = "storage_location_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_storage_location_config",
        columnNames = {"storage_location_id", "product_id", "packaging_catalog_id"}
    )
)
public class StorageLocationConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_catalog_id")
    private PackagingCatalog packagingCatalog;

    @Column(nullable = false)
    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Getters
    public StorageLocation getStorageLocation() { return storageLocation; }
    public Product getProduct() { return product; }
    public PackagingCatalog getPackagingCatalog() { return packagingCatalog; }
    public boolean isActive() { return active; }
    public String getNotes() { return notes; }

    // Setters
    public void setStorageLocation(StorageLocation storageLocation) { this.storageLocation = storageLocation; }
    public void setProduct(Product product) { this.product = product; }
    public void setPackagingCatalog(PackagingCatalog packagingCatalog) { this.packagingCatalog = packagingCatalog; }
    public void setActive(boolean active) { this.active = active; }
    public void setNotes(String notes) { this.notes = notes; }
}
