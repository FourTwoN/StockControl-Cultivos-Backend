package com.fortytwo.demeter.inventario.model;

/**
 * Source of a stock movement - whether it was initiated by a user or by the ML system.
 */
public enum SourceType {
    MANUAL,  // User-initiated movement
    IA       // ML/AI-generated movement (from photo processing)
}
