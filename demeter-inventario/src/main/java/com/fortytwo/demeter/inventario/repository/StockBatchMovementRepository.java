package com.fortytwo.demeter.inventario.repository;

import com.fortytwo.demeter.inventario.model.StockBatchMovement;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class StockBatchMovementRepository implements PanacheRepositoryBase<StockBatchMovement, UUID> {

    public List<StockBatchMovement> findByBatchId(UUID batchId) {
        return find("batch.id", batchId).list();
    }

    public List<StockBatchMovement> findByMovementId(UUID movementId) {
        return find("movement.id", movementId).list();
    }

    /**
     * Find all batch-movements where this batch was the cycle initiator.
     */
    public List<StockBatchMovement> findCycleInitiatorsByBatch(UUID batchId) {
        return find("batch.id = ?1 AND isCycleInitiator = true", batchId).list();
    }

    /**
     * Find batch-movements ordered by movementOrder for a given movement.
     */
    public List<StockBatchMovement> findByMovementIdOrdered(UUID movementId) {
        return find("movement.id = ?1 ORDER BY movementOrder", movementId).list();
    }
}
