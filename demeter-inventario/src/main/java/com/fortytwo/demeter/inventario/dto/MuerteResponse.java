package com.fortytwo.demeter.inventario.dto;

import java.util.UUID;

/**
 * Response for mortality registration.
 * Contains the created movement and updated batch information.
 */
public record MuerteResponse(
    StockMovementDTO movement,
    UUID batchId,
    Integer quantityRemoved,
    Integer newQuantity
) {}
