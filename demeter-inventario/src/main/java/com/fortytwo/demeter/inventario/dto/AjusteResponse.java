package com.fortytwo.demeter.inventario.dto;

import java.util.UUID;

/**
 * Response for stock adjustment.
 * Contains the created movement and updated batch information.
 */
public record AjusteResponse(
    StockMovementDTO movement,
    UUID batchId,
    Integer quantityAdjusted,
    Integer newQuantity
) {}
