package com.fortytwo.demeter.inventario.dto;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record UpdateStockBatchRequest(
    UUID storageLocationId,
    String productState,
    UUID productSizeId,
    UUID packagingCatalogId,
    @Min(0) Integer quantityCurrent,
    LocalDate plantingDate,
    LocalDate germinationDate,
    LocalDate transplantDate,
    LocalDate expectedReadyDate,
    BigDecimal qualityScore,
    String notes,
    Map<String, Object> customAttributes,
    String status
) {}
