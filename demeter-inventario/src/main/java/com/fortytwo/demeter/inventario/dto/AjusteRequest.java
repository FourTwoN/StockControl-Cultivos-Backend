package com.fortytwo.demeter.inventario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request for stock adjustment (correction).
 * Quantity can be positive (add) or negative (subtract), but NOT zero.
 */
public record AjusteRequest(
    @NotNull(message = "batchId is required")
    UUID batchId,

    @NotNull(message = "quantity is required")
    Integer quantity,  // Can be + or -, validated in service

    @Size(max = 500, message = "reasonDescription must not exceed 500 characters")
    String reasonDescription
) {}
