package com.fortytwo.demeter.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record CreateStockBatchRequest(
    @NotNull UUID productId,
    @NotBlank @Size(max = 100) String batchCode,
    @NotNull UUID storageLocationId,
    @NotBlank String productState,
    UUID productSizeId,
    UUID packagingCatalogId,
    @NotNull @Min(0) Integer quantity,
    LocalDate plantingDate,
    LocalDate germinationDate,
    LocalDate transplantDate,
    LocalDate expectedReadyDate,
    BigDecimal qualityScore,
    String notes,
    Map<String, Object> customAttributes,
    Instant cycleStartDate
) {}
