package com.fortytwo.demeter.inventario.model;

/**
 * Types of stock movements in the inventory system.
 */
public enum MovementType {
    // Inbound operations
    FOTO,                  // ML photo-based stock initialization
    MANUAL_INIT,           // Manual stock initialization
    PLANTADO,              // New planting (inbound)

    // Outbound operations
    MUERTE,                // Plant death (outbound/loss)
    VENTA,                 // Sale (outbound, auto-calculated from cycle changes)

    // Transfer/Change operations
    MOVIMIENTO,            // Pure location change
    TRASPLANTE,            // Config change within same location (size/state change)
    MOVIMIENTO_TRASPLANTE, // Location + config change

    // Adjustments
    AJUSTE,                // Manual adjustment (+/-)

    // Legacy (deprecated - use specific types above)
    @Deprecated
    ENTRADA                // Legacy: use MANUAL_INIT or PLANTADO instead
}
