package com.fortytwo.demeter.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request for registering plant mortality (egreso).
 */
public record MuerteRequest(
    @NotNull(message = "batchId is required")
    UUID batchId,

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    Integer quantity,

    @Size(max = 500, message = "reasonDescription must not exceed 500 characters")
    String reasonDescription
) {}
