package com.fortytwo.demeter.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateStockMovementRequest(
    @NotNull String movementType,
    @NotNull @Min(1) Integer quantity,
    boolean isInbound,
    @NotNull UUID userId,
    @NotNull String sourceType,
    String reasonDescription,
    UUID processingSessionId,
    UUID parentMovementId,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    Instant performedAt,
    @NotEmpty @Valid List<BatchQuantity> batchQuantities
) {
    public record BatchQuantity(
        @NotNull UUID batchId,
        @NotNull @Min(1) BigDecimal quantity
    ) {}
}
