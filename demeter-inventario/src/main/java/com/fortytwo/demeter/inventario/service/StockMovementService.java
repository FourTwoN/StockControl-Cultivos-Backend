package com.fortytwo.demeter.inventario.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.fotos.repository.PhotoProcessingSessionRepository;
import com.fortytwo.demeter.inventario.dto.CreateStockMovementRequest;
import com.fortytwo.demeter.inventario.dto.StockMovementDTO;
import com.fortytwo.demeter.inventario.model.BatchStatus;
import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.model.SourceType;
import com.fortytwo.demeter.inventario.model.StockBatch;
import com.fortytwo.demeter.inventario.model.StockBatchMovement;
import com.fortytwo.demeter.inventario.model.StockMovement;
import com.fortytwo.demeter.inventario.repository.StockBatchMovementRepository;
import com.fortytwo.demeter.inventario.repository.StockBatchRepository;
import com.fortytwo.demeter.inventario.repository.StockMovementRepository;
import com.fortytwo.demeter.usuarios.repository.UserRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class StockMovementService {

    private static final Logger log = LoggerFactory.getLogger(StockMovementService.class);

    @Inject
    StockMovementRepository stockMovementRepository;

    @Inject
    StockBatchRepository stockBatchRepository;

    @Inject
    StockBatchMovementRepository stockBatchMovementRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    PhotoProcessingSessionRepository photoProcessingSessionRepository;

    public PagedResponse<StockMovementDTO> findAll(int page, int size, UUID batchId, String type, Instant startDate, Instant endDate) {
        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;
        if (batchId != null) {
            query.append(" and referenceId = ?").append(paramIndex++);
            params.add(batchId);
        }
        if (type != null && !type.isBlank()) {
            query.append(" and movementType = ?").append(paramIndex++);
            params.add(MovementType.valueOf(type));
        }
        if (startDate != null) {
            query.append(" and performedAt >= ?").append(paramIndex++);
            params.add(startDate);
        }
        if (endDate != null) {
            query.append(" and performedAt <= ?").append(paramIndex++);
            params.add(endDate);
        }
        String jpql = query.toString();
        long total = stockMovementRepository.count(jpql, params.toArray());
        var movements = stockMovementRepository.find(jpql + " order by performedAt desc", params.toArray())
                .page(Page.of(page, size)).list();
        return PagedResponse.of(movements.stream().map(StockMovementDTO::from).toList(), page, size, total);
    }

    public StockMovementDTO findById(UUID id) {
        StockMovement movement = stockMovementRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockMovement", id));
        return StockMovementDTO.from(movement);
    }

    public List<StockMovementDTO> findByMovementType(MovementType type) {
        return stockMovementRepository.findByMovementType(type)
                .stream().map(StockMovementDTO::from).toList();
    }

    public List<StockMovementDTO> findByDateRange(Instant from, Instant to) {
        return stockMovementRepository.findByDateRange(from, to)
                .stream().map(StockMovementDTO::from).toList();
    }

    public List<StockMovementDTO> findByReferenceId(UUID referenceId) {
        return stockMovementRepository.findByReferenceId(referenceId)
                .stream().map(StockMovementDTO::from).toList();
    }

    @Transactional
    public StockMovementDTO create(CreateStockMovementRequest request) {
        MovementType movementType = MovementType.valueOf(request.movementType());
        SourceType sourceType = SourceType.valueOf(request.sourceType());

        StockMovement movement = new StockMovement();
        movement.setMovementType(movementType);
        movement.setQuantity(request.quantity());
        movement.setInbound(request.isInbound());
        movement.setUser(userRepository.findByIdOptional(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User", request.userId())));
        movement.setSourceType(sourceType);
        movement.setReasonDescription(request.reasonDescription());

        if (request.processingSessionId() != null) {
            movement.setProcessingSession(photoProcessingSessionRepository.findByIdOptional(request.processingSessionId())
                    .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", request.processingSessionId())));
        }
        if (request.parentMovementId() != null) {
            movement.setParentMovement(stockMovementRepository.findByIdOptional(request.parentMovementId())
                    .orElseThrow(() -> new EntityNotFoundException("StockMovement", request.parentMovementId())));
        }

        movement.setUnitPrice(request.unitPrice());
        movement.setTotalPrice(request.totalPrice());
        movement.setPerformedAt(request.performedAt() != null ? request.performedAt() : Instant.now());

        stockMovementRepository.persist(movement);

        for (CreateStockMovementRequest.BatchQuantity bq : request.batchQuantities()) {
            StockBatch batch = stockBatchRepository.findByIdOptional(bq.batchId())
                    .orElseThrow(() -> new EntityNotFoundException("StockBatch", bq.batchId()));

            StockBatchMovement batchMovement = new StockBatchMovement();
            batchMovement.setBatch(batch);
            batchMovement.setMovement(movement);
            batchMovement.setQuantity(bq.quantity());
            stockBatchMovementRepository.persist(batchMovement);

            applyQuantityChange(batch, movementType, bq.quantity());
        }

        log.info("Created stock movement type={} source={} with {} batch entries",
                movementType, sourceType, request.batchQuantities().size());

        return StockMovementDTO.from(movement);
    }

    private void applyQuantityChange(StockBatch batch, MovementType movementType, BigDecimal movementQuantity) {
        int currentQuantity = batch.getQuantityCurrent();
        int movementQty = movementQuantity.intValue();
        int newQuantity;

        switch (movementType) {
            case FOTO, MANUAL_INIT, PLANTADO, ENTRADA -> newQuantity = currentQuantity + movementQty;
            case MUERTE, VENTA -> newQuantity = currentQuantity - movementQty;
            case MOVIMIENTO, TRASPLANTE, MOVIMIENTO_TRASPLANTE -> newQuantity = currentQuantity; // No change to source batch
            case AJUSTE -> newQuantity = movementQty;
            default -> throw new IllegalArgumentException("Unknown movement type: " + movementType);
        }

        batch.setQuantityCurrent(newQuantity);

        if (newQuantity <= 0) {
            batch.setStatus(BatchStatus.DEPLETED);
            log.info("Batch {} depleted after movement", batch.getBatchCode());
        }
    }
}
