package com.fortytwo.demeter.inventario.dto;

import java.util.UUID;

/**
 * Response for planting registration.
 * Contains the created movement and updated batch information.
 */
public record PlantadoResponse(
    StockMovementDTO movement,
    UUID batchId,
    String batchCode,
    UUID locationId,
    Integer newQuantity
) {}
