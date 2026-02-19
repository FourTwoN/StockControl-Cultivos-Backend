package com.fortytwo.demeter.inventario.repository;

import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.model.SourceType;
import com.fortytwo.demeter.inventario.model.StockMovement;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class StockMovementRepository implements PanacheRepositoryBase<StockMovement, UUID> {

    public List<StockMovement> findByMovementType(MovementType type) {
        return find("movementType", type).list();
    }

    public List<StockMovement> findByDateRange(Instant from, Instant to) {
        return find("performedAt >= ?1 and performedAt <= ?2", from, to).list();
    }

    public List<StockMovement> findBySourceType(SourceType sourceType) {
        return find("sourceType", sourceType).list();
    }

    public List<StockMovement> findByProcessingSession(UUID processingSessionId) {
        return find("processingSession.id", processingSessionId).list();
    }

    public List<StockMovement> findByUser(UUID userId) {
        return find("user.id", userId).list();
    }

    // Legacy method - deprecated
    @Deprecated
    public List<StockMovement> findByReferenceId(UUID referenceId) {
        return find("referenceId", referenceId).list();
    }
}
