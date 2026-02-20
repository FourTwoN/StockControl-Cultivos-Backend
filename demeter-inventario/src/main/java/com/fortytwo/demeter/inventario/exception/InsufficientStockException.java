package com.fortytwo.demeter.inventario.exception;

import java.util.UUID;

/**
 * Thrown when attempting to remove more stock than available in a batch.
 */
public class InsufficientStockException extends RuntimeException {

    private final UUID batchId;
    private final Integer requested;
    private final Integer available;

    public InsufficientStockException(UUID batchId, Integer requested, Integer available) {
        super("Batch %s has insufficient stock: requested %d, available %d"
              .formatted(batchId, requested, available));
        this.batchId = batchId;
        this.requested = requested;
        this.available = available;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public Integer getRequested() {
        return requested;
    }

    public Integer getAvailable() {
        return available;
    }
}
