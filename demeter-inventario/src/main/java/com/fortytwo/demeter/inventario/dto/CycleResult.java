package com.fortytwo.demeter.inventario.dto;

import com.fortytwo.demeter.inventario.model.StockBatch;

/**
 * Result of starting a new stock cycle.
 *
 * @param newBatch The newly created batch for this cycle
 * @param salesInfo Information about automatic sales detection (null if first cycle or no change)
 */
public record CycleResult(
    StockBatch newBatch,
    SalesInfo salesInfo
) {}
