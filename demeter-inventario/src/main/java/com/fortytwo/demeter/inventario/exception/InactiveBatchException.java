package com.fortytwo.demeter.inventario.exception;

import java.util.UUID;

/**
 * Thrown when attempting to operate on a batch that has a closed cycle.
 * A batch is inactive when cycleEndDate is NOT NULL.
 */
public class InactiveBatchException extends RuntimeException {

    private final UUID batchId;
    private final Integer cycleNumber;

    public InactiveBatchException(UUID batchId, Integer cycleNumber) {
        super("Batch %s is inactive (cycle %d already closed)".formatted(batchId, cycleNumber));
        this.batchId = batchId;
        this.cycleNumber = cycleNumber;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public Integer getCycleNumber() {
        return cycleNumber;
    }
}
