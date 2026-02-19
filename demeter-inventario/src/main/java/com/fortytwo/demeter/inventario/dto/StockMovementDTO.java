package com.fortytwo.demeter.inventario.dto;

import com.fortytwo.demeter.inventario.model.StockBatchMovement;
import com.fortytwo.demeter.inventario.model.StockMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockMovementDTO(
    UUID id,
    String movementType,
    Integer quantity,
    boolean isInbound,
    UUID userId,
    String userName,
    String sourceType,
    String reasonDescription,
    UUID processingSessionId,
    UUID parentMovementId,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    Instant performedAt,
    List<BatchMovementDetail> batchMovements,
    Instant createdAt,
    Instant updatedAt
) {
    public record BatchMovementDetail(
        UUID id,
        UUID batchId,
        String batchCode,
        BigDecimal quantity,
        Instant createdAt
    ) {
        public static BatchMovementDetail from(StockBatchMovement bm) {
            return new BatchMovementDetail(
                bm.getId(),
                bm.getBatch() != null ? bm.getBatch().getId() : null,
                bm.getBatch() != null ? bm.getBatch().getBatchCode() : null,
                bm.getQuantity(),
                bm.getCreatedAt()
            );
        }
    }

    public static StockMovementDTO from(StockMovement m) {
        List<BatchMovementDetail> details = m.getBatchMovements() != null
            ? m.getBatchMovements().stream().map(BatchMovementDetail::from).toList()
            : List.of();

        return new StockMovementDTO(
            m.getId(),
            m.getMovementType().name(),
            m.getQuantity(),
            m.isInbound(),
            m.getUser() != null ? m.getUser().getId() : null,
            m.getUser() != null ? m.getUser().getName() : null,
            m.getSourceType() != null ? m.getSourceType().name() : null,
            m.getReasonDescription(),
            m.getProcessingSession() != null ? m.getProcessingSession().getId() : null,
            m.getParentMovement() != null ? m.getParentMovement().getId() : null,
            m.getUnitPrice(),
            m.getTotalPrice(),
            m.getPerformedAt(),
            details,
            m.getCreatedAt(),
            m.getUpdatedAt()
        );
    }
}
