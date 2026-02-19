package com.fortytwo.demeter.inventario.model;

import com.fortytwo.demeter.common.model.BaseEntity;
import com.fortytwo.demeter.empaquetado.model.PackagingCatalog;
import com.fortytwo.demeter.productos.model.Product;
import com.fortytwo.demeter.productos.model.ProductSize;
import com.fortytwo.demeter.productos.model.ProductState;
import com.fortytwo.demeter.ubicaciones.model.StorageLocation;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Represents a batch of stock items at a specific location with cycle tracking.
 * Only ONE active batch per (location, product, state, size, packaging) combination.
 * Active batches have cycleEndDate = null.
 */
@Entity
@Table(name = "stock_batches")
public class StockBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_storage_location_id", nullable = false)
    private StorageLocation currentStorageLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_state", nullable = false, length = 50)
    private ProductState productState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id")
    private ProductSize productSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_catalog_id")
    private PackagingCatalog packagingCatalog;

    // Cycle tracking
    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber = 1;

    @Column(name = "cycle_start_date", nullable = false)
    private Instant cycleStartDate;

    @Column(name = "cycle_end_date")
    private Instant cycleEndDate;

    // Quantity tracking (integer counts for plants)
    @Column(name = "quantity_initial", nullable = false)
    private Integer quantityInitial;

    @Column(name = "quantity_current", nullable = false)
    private Integer quantityCurrent;

    // Growth tracking (optional)
    @Column(name = "planting_date")
    private LocalDate plantingDate;

    @Column(name = "germination_date")
    private LocalDate germinationDate;

    @Column(name = "transplant_date")
    private LocalDate transplantDate;

    @Column(name = "expected_ready_date")
    private LocalDate expectedReadyDate;

    // Quality
    @Column(name = "quality_score", precision = 3, scale = 2)
    private BigDecimal qualityScore;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes", columnDefinition = "jsonb")
    private Map<String, Object> customAttributes;

    // Legacy fields (kept for backward compatibility during migration)
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BatchStatus status = BatchStatus.ACTIVE;

    // Getters
    public Product getProduct() { return product; }
    public String getBatchCode() { return batchCode; }
    public StorageLocation getCurrentStorageLocation() { return currentStorageLocation; }
    public ProductState getProductState() { return productState; }
    public ProductSize getProductSize() { return productSize; }
    public PackagingCatalog getPackagingCatalog() { return packagingCatalog; }
    public Integer getCycleNumber() { return cycleNumber; }
    public Instant getCycleStartDate() { return cycleStartDate; }
    public Instant getCycleEndDate() { return cycleEndDate; }
    public Integer getQuantityInitial() { return quantityInitial; }
    public Integer getQuantityCurrent() { return quantityCurrent; }
    public LocalDate getPlantingDate() { return plantingDate; }
    public LocalDate getGerminationDate() { return germinationDate; }
    public LocalDate getTransplantDate() { return transplantDate; }
    public LocalDate getExpectedReadyDate() { return expectedReadyDate; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public String getNotes() { return notes; }
    public Map<String, Object> getCustomAttributes() { return customAttributes; }
    public BatchStatus getStatus() { return status; }

    // Setters
    public void setProduct(Product product) { this.product = product; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public void setCurrentStorageLocation(StorageLocation currentStorageLocation) { this.currentStorageLocation = currentStorageLocation; }
    public void setProductState(ProductState productState) { this.productState = productState; }
    public void setProductSize(ProductSize productSize) { this.productSize = productSize; }
    public void setPackagingCatalog(PackagingCatalog packagingCatalog) { this.packagingCatalog = packagingCatalog; }
    public void setCycleNumber(Integer cycleNumber) { this.cycleNumber = cycleNumber; }
    public void setCycleStartDate(Instant cycleStartDate) { this.cycleStartDate = cycleStartDate; }
    public void setCycleEndDate(Instant cycleEndDate) { this.cycleEndDate = cycleEndDate; }
    public void setQuantityInitial(Integer quantityInitial) { this.quantityInitial = quantityInitial; }
    public void setQuantityCurrent(Integer quantityCurrent) { this.quantityCurrent = quantityCurrent; }
    public void setPlantingDate(LocalDate plantingDate) { this.plantingDate = plantingDate; }
    public void setGerminationDate(LocalDate germinationDate) { this.germinationDate = germinationDate; }
    public void setTransplantDate(LocalDate transplantDate) { this.transplantDate = transplantDate; }
    public void setExpectedReadyDate(LocalDate expectedReadyDate) { this.expectedReadyDate = expectedReadyDate; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCustomAttributes(Map<String, Object> customAttributes) { this.customAttributes = customAttributes; }
    public void setStatus(BatchStatus status) { this.status = status; }

    /**
     * Check if this batch is currently active (not closed).
     */
    public boolean isActive() {
        return cycleEndDate == null;
    }
}
