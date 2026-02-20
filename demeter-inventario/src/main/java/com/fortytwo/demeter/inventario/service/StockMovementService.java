package com.fortytwo.demeter.inventario.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.fotos.repository.PhotoProcessingSessionRepository;
import com.fortytwo.demeter.inventario.dto.*;
import com.fortytwo.demeter.inventario.exception.InactiveBatchException;
import com.fortytwo.demeter.inventario.exception.InsufficientStockException;
import com.fortytwo.demeter.inventario.model.BatchStatus;
import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.model.SourceType;
import com.fortytwo.demeter.inventario.model.StockBatch;
import com.fortytwo.demeter.inventario.model.StockBatchMovement;
import com.fortytwo.demeter.inventario.model.StockMovement;
import com.fortytwo.demeter.inventario.repository.StockBatchMovementRepository;
import com.fortytwo.demeter.inventario.repository.StockBatchRepository;
import com.fortytwo.demeter.inventario.repository.StockMovementRepository;
import com.fortytwo.demeter.usuarios.model.User;
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
import java.util.Objects;
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

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void validateBatchIsActive(StockBatch batch) {
        if (!batch.isActive()) {
            throw new InactiveBatchException(batch.getId(), batch.getCycleNumber());
        }
    }

    private void validateSufficientStock(StockBatch batch, int requested) {
        if (batch.getQuantityCurrent() < requested) {
            throw new InsufficientStockException(batch.getId(), requested, batch.getQuantityCurrent());
        }
    }

    private MovementType detectMovementType(StockBatch source, StockBatch dest) {
        if (!source.getProduct().getId().equals(dest.getProduct().getId())) {
            throw new IllegalArgumentException("Cannot move between different products: source=" +
                source.getProduct().getId() + ", dest=" + dest.getProduct().getId());
        }

        boolean sameLocation = source.getCurrentStorageLocation().getId()
                                     .equals(dest.getCurrentStorageLocation().getId());
        boolean configChanged = !Objects.equals(source.getProductState(), dest.getProductState())
                             || !Objects.equals(
                                    source.getProductSize() != null ? source.getProductSize().getId() : null,
                                    dest.getProductSize() != null ? dest.getProductSize().getId() : null)
                             || !Objects.equals(
                                    source.getPackagingCatalog() != null ? source.getPackagingCatalog().getId() : null,
                                    dest.getPackagingCatalog() != null ? dest.getPackagingCatalog().getId() : null);

        if (sameLocation && configChanged) return MovementType.TRASPLANTE;
        if (!sameLocation && configChanged) return MovementType.MOVIMIENTO_TRASPLANTE;
        if (!sameLocation && !configChanged) return MovementType.MOVIMIENTO;

        throw new IllegalArgumentException("Invalid desplazamiento: same location with identical config");
    }

    private void linkMovementToBatch(StockMovement movement, StockBatch batch, boolean isCycleInitiator) {
        long count = stockBatchMovementRepository.count("batch.id = ?1", batch.getId());
        int nextOrder = (int) count + 1;

        StockBatchMovement link = new StockBatchMovement();
        link.setBatch(batch);
        link.setMovement(movement);
        link.setQuantity(BigDecimal.valueOf(Math.abs(movement.getQuantity())));
        link.setCycleInitiator(isCycleInitiator);
        link.setMovementOrder(nextOrder);
        stockBatchMovementRepository.persist(link);
    }

    // ═══════════════════════════════════════════════════════════════
    // SPECIALIZED MOVEMENT OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public MuerteResponse executeMuerte(UUID userId, MuerteRequest request) {
        StockBatch batch = stockBatchRepository.findByIdOptional(request.batchId())
            .orElseThrow(() -> new EntityNotFoundException("StockBatch", request.batchId()));

        validateBatchIsActive(batch);
        validateSufficientStock(batch, request.quantity());

        User user = userRepository.findByIdOptional(userId)
            .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // PHASE 1: Create movement
        StockMovement movement = new StockMovement();
        movement.setMovementType(MovementType.MUERTE);
        movement.setQuantity(-request.quantity());  // Negative for egreso
        movement.setInbound(false);
        movement.setUser(user);
        movement.setSourceType(SourceType.MANUAL);
        movement.setReasonDescription(request.reasonDescription() != null
            ? request.reasonDescription() : "Plant death");
        movement.setPerformedAt(Instant.now());
        stockMovementRepository.persist(movement);

        // PHASE 2: Link to batch
        linkMovementToBatch(movement, batch, false);

        // Update batch quantity
        batch.setQuantityCurrent(batch.getQuantityCurrent() - request.quantity());
        if (batch.getQuantityCurrent() <= 0) {
            batch.setStatus(BatchStatus.DEPLETED);
        }

        log.info("MUERTE: {} plants removed from batch {}", request.quantity(), batch.getBatchCode());

        return new MuerteResponse(
            StockMovementDTO.from(movement),
            batch.getId(),
            request.quantity(),
            batch.getQuantityCurrent()
        );
    }

    @Transactional
    public PlantadoResponse executePlantado(UUID userId, PlantadoRequest request) {
        StockBatch batch = stockBatchRepository.findByIdOptional(request.batchId())
            .orElseThrow(() -> new EntityNotFoundException("StockBatch", request.batchId()));

        validateBatchIsActive(batch);

        User user = userRepository.findByIdOptional(userId)
            .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // PHASE 1: Create movement
        StockMovement movement = new StockMovement();
        movement.setMovementType(MovementType.PLANTADO);
        movement.setQuantity(request.quantity());  // Positive for ingreso
        movement.setInbound(true);
        movement.setUser(user);
        movement.setSourceType(SourceType.MANUAL);
        movement.setReasonDescription(request.reasonDescription() != null
            ? request.reasonDescription() : "New planting");
        movement.setPerformedAt(Instant.now());
        stockMovementRepository.persist(movement);

        // PHASE 2: Link to batch
        linkMovementToBatch(movement, batch, false);

        // Update batch quantity
        batch.setQuantityCurrent(batch.getQuantityCurrent() + request.quantity());

        log.info("PLANTADO: {} plants added to batch {}", request.quantity(), batch.getBatchCode());

        return new PlantadoResponse(
            StockMovementDTO.from(movement),
            batch.getId(),
            batch.getBatchCode(),
            batch.getCurrentStorageLocation().getId(),
            batch.getQuantityCurrent()
        );
    }

    @Transactional
    public AjusteResponse executeAjuste(UUID userId, AjusteRequest request) {
        if (request.quantity() == 0) {
            throw new IllegalArgumentException("Adjustment quantity cannot be zero");
        }

        StockBatch batch = stockBatchRepository.findByIdOptional(request.batchId())
            .orElseThrow(() -> new EntityNotFoundException("StockBatch", request.batchId()));

        validateBatchIsActive(batch);

        // If subtracting, validate sufficient stock
        if (request.quantity() < 0) {
            validateSufficientStock(batch, Math.abs(request.quantity()));
        }

        User user = userRepository.findByIdOptional(userId)
            .orElseThrow(() -> new EntityNotFoundException("User", userId));

        boolean isInbound = request.quantity() > 0;

        // PHASE 1: Create movement
        StockMovement movement = new StockMovement();
        movement.setMovementType(MovementType.AJUSTE);
        movement.setQuantity(request.quantity());
        movement.setInbound(isInbound);
        movement.setUser(user);
        movement.setSourceType(SourceType.MANUAL);
        movement.setReasonDescription(request.reasonDescription() != null
            ? request.reasonDescription() : "Stock adjustment");
        movement.setPerformedAt(Instant.now());
        stockMovementRepository.persist(movement);

        // PHASE 2: Link to batch
        linkMovementToBatch(movement, batch, false);

        // Update batch quantity
        batch.setQuantityCurrent(batch.getQuantityCurrent() + request.quantity());
        if (batch.getQuantityCurrent() <= 0) {
            batch.setStatus(BatchStatus.DEPLETED);
        }

        log.info("AJUSTE: {} plants adjusted in batch {}", request.quantity(), batch.getBatchCode());

        return new AjusteResponse(
            StockMovementDTO.from(movement),
            batch.getId(),
            request.quantity(),
            batch.getQuantityCurrent()
        );
    }

    @Transactional
    public DesplazamientoResponse executeDesplazamiento(UUID userId, DesplazamientoRequest request) {
        // PHASE 1: Get and validate both batches
        StockBatch sourceBatch = stockBatchRepository.findByIdOptional(request.sourceBatchId())
            .orElseThrow(() -> new EntityNotFoundException("StockBatch", request.sourceBatchId()));
        StockBatch destBatch = stockBatchRepository.findByIdOptional(request.destinationBatchId())
            .orElseThrow(() -> new EntityNotFoundException("StockBatch", request.destinationBatchId()));

        validateBatchIsActive(sourceBatch);
        validateBatchIsActive(destBatch);
        validateSufficientStock(sourceBatch, request.quantity());

        User user = userRepository.findByIdOptional(userId)
            .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // PHASE 2: Auto-detect movement type
        MovementType movementType = detectMovementType(sourceBatch, destBatch);

        log.info("Executing {}: {} plants from batch {} to {}",
            movementType, request.quantity(), sourceBatch.getBatchCode(), destBatch.getBatchCode());

        // PHASE 3: Create EGRESO movement (subtract from source)
        StockMovement egresoMovement = new StockMovement();
        egresoMovement.setMovementType(movementType);
        egresoMovement.setQuantity(-request.quantity());
        egresoMovement.setInbound(false);
        egresoMovement.setUser(user);
        egresoMovement.setSourceType(SourceType.MANUAL);
        egresoMovement.setReasonDescription(request.reasonDescription() != null
            ? request.reasonDescription()
            : "%s: Egreso from %s".formatted(movementType, sourceBatch.getBatchCode()));
        egresoMovement.setPerformedAt(Instant.now());
        stockMovementRepository.persist(egresoMovement);

        // PHASE 4: Create INGRESO movement (add to destination)
        StockMovement ingresoMovement = new StockMovement();
        ingresoMovement.setMovementType(movementType);
        ingresoMovement.setQuantity(request.quantity());
        ingresoMovement.setInbound(true);
        ingresoMovement.setUser(user);
        ingresoMovement.setSourceType(SourceType.MANUAL);
        ingresoMovement.setReasonDescription(request.reasonDescription() != null
            ? request.reasonDescription()
            : "%s: Ingreso to %s".formatted(movementType, destBatch.getBatchCode()));
        ingresoMovement.setPerformedAt(Instant.now());
        ingresoMovement.setParentMovement(egresoMovement);  // Link ingreso to egreso
        stockMovementRepository.persist(ingresoMovement);

        // PHASE 5: Link movements to batches
        linkMovementToBatch(egresoMovement, sourceBatch, false);
        linkMovementToBatch(ingresoMovement, destBatch, false);

        // PHASE 6: Update batch quantities
        sourceBatch.setQuantityCurrent(sourceBatch.getQuantityCurrent() - request.quantity());
        destBatch.setQuantityCurrent(destBatch.getQuantityCurrent() + request.quantity());

        if (sourceBatch.getQuantityCurrent() <= 0) {
            sourceBatch.setStatus(BatchStatus.DEPLETED);
        }

        log.info("{} completed: {} plants moved", movementType, request.quantity());

        return new DesplazamientoResponse(
            movementType.name().toLowerCase(),
            new DesplazamientoResponse.MovementPair(
                StockMovementDTO.from(egresoMovement),
                StockMovementDTO.from(ingresoMovement)
            ),
            new DesplazamientoResponse.BatchInfo(
                sourceBatch.getId(),
                sourceBatch.getBatchCode(),
                sourceBatch.getCurrentStorageLocation().getId(),
                sourceBatch.getQuantityCurrent()
            ),
            new DesplazamientoResponse.BatchInfo(
                destBatch.getId(),
                destBatch.getBatchCode(),
                destBatch.getCurrentStorageLocation().getId(),
                destBatch.getQuantityCurrent()
            ),
            request.quantity(),
            Instant.now()
        );
    }
}
