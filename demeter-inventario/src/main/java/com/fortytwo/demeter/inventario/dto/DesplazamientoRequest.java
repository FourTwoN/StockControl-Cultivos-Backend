package com.fortytwo.demeter.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request for displacement/movement operation.
 *
 * Backend auto-detects operation type by comparing batch configurations:
 * - MOVIMIENTO: Different location, same config
 * - MOVIMIENTO_TRASPLANTE: Different location, different config
 * - TRASPLANTE: Same location, different config
 */
public record DesplazamientoRequest(
    @NotNull(message = "sourceBatchId is required")
    UUID sourceBatchId,

    @NotNull(message = "destinationBatchId is required")
    UUID destinationBatchId,

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    Integer quantity,

    @Size(max = 500, message = "reasonDescription must not exceed 500 characters")
    String reasonDescription
) {}
