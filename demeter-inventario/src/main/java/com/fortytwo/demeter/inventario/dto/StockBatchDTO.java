package com.fortytwo.demeter.inventario.dto;

import com.fortytwo.demeter.inventario.model.StockBatch;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record StockBatchDTO(
    UUID id,
    UUID productId,
    String productName,
    String batchCode,
    UUID storageLocationId,
    String storageLocationName,
    String productState,
    UUID productSizeId,
    String productSizeLabel,
    UUID packagingCatalogId,
    String packagingCatalogName,
    Integer cycleNumber,
    Instant cycleStartDate,
    Instant cycleEndDate,
    Integer quantityInitial,
    Integer quantityCurrent,
    LocalDate plantingDate,
    LocalDate germinationDate,
    LocalDate transplantDate,
    LocalDate expectedReadyDate,
    BigDecimal qualityScore,
    String notes,
    Map<String, Object> customAttributes,
    String status,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static StockBatchDTO from(StockBatch b) {
        return new StockBatchDTO(
            b.getId(),
            b.getProduct() != null ? b.getProduct().getId() : null,
            b.getProduct() != null ? b.getProduct().getName() : null,
            b.getBatchCode(),
            b.getCurrentStorageLocation() != null ? b.getCurrentStorageLocation().getId() : null,
            b.getCurrentStorageLocation() != null ? b.getCurrentStorageLocation().getName() : null,
            b.getProductState() != null ? b.getProductState().name() : null,
            b.getProductSize() != null ? b.getProductSize().getId() : null,
            b.getProductSize() != null ? b.getProductSize().getLabel() : null,
            b.getPackagingCatalog() != null ? b.getPackagingCatalog().getId() : null,
            b.getPackagingCatalog() != null ? b.getPackagingCatalog().getName() : null,
            b.getCycleNumber(),
            b.getCycleStartDate(),
            b.getCycleEndDate(),
            b.getQuantityInitial(),
            b.getQuantityCurrent(),
            b.getPlantingDate(),
            b.getGerminationDate(),
            b.getTransplantDate(),
            b.getExpectedReadyDate(),
            b.getQualityScore(),
            b.getNotes(),
            b.getCustomAttributes(),
            b.getStatus() != null ? b.getStatus().name() : null,
            b.isActive(),
            b.getCreatedAt(),
            b.getUpdatedAt()
        );
    }
}
