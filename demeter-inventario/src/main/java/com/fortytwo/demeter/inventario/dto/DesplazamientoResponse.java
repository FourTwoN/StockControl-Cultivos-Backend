package com.fortytwo.demeter.inventario.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for displacement/movement operation.
 * All 3 types (movimiento, trasplante, movimiento_trasplante) return 2 movements.
 */
public record DesplazamientoResponse(
    String operationType,  // "movimiento" | "trasplante" | "movimiento_trasplante"
    MovementPair movements,
    BatchInfo sourceBatch,
    BatchInfo destinationBatch,
    Integer quantity,
    Instant createdAt
) {
    /**
     * Pair of movements created for the displacement operation.
     */
    public record MovementPair(
        StockMovementDTO egreso,
        StockMovementDTO ingreso
    ) {}

    /**
     * Batch information after the displacement operation.
     */
    public record BatchInfo(
        UUID batchId,
        String batchCode,
        UUID locationId,
        Integer newQuantity
    ) {}
}
