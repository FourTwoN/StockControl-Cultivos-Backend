package com.fortytwo.demeter.app.service;

import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.fotos.model.Estimation;
import com.fortytwo.demeter.fotos.model.PhotoProcessingSession;
import com.fortytwo.demeter.fotos.repository.EstimationRepository;
import com.fortytwo.demeter.fotos.repository.PhotoProcessingSessionRepository;
import com.fortytwo.demeter.inventario.dto.CycleResult;
import com.fortytwo.demeter.inventario.dto.SalesInfo;
import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.model.SourceType;
import com.fortytwo.demeter.inventario.model.StockBatch;
import com.fortytwo.demeter.inventario.model.StockBatchMovement;
import com.fortytwo.demeter.inventario.model.StockMovement;
import com.fortytwo.demeter.inventario.repository.StockBatchMovementRepository;
import com.fortytwo.demeter.inventario.repository.StockMovementRepository;
import com.fortytwo.demeter.inventario.service.StockBatchService;
import com.fortytwo.demeter.productos.model.ProductState;
import com.fortytwo.demeter.ubicaciones.model.StorageLocationConfig;
import com.fortytwo.demeter.ubicaciones.service.StorageLocationConfigService;
import com.fortytwo.demeter.usuarios.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates stock updates after photo processing session completion.
 *
 * <p>This service bridges demeter-fotos (ML results) and demeter-inventario (stock management)
 * to enable automatic stock cycle management based on photo detection counts.
 *
 * <p>Flow:
 * <ol>
 *   <li>Get storage location configs for the session's location</li>
 *   <li>Extract count from COUNT-type estimations</li>
 *   <li>Start new cycle for each config (with auto-sales detection)</li>
 *   <li>Create FOTO movement linking all new batches</li>
 * </ol>
 */
@ApplicationScoped
public class StockUpdateOrchestrator {

    private static final Logger log = Logger.getLogger(StockUpdateOrchestrator.class);
    private static final String ESTIMATION_TYPE_COUNT = "COUNT";

    @Inject
    PhotoProcessingSessionRepository sessionRepository;

    @Inject
    EstimationRepository estimationRepository;

    @Inject
    StockBatchService stockBatchService;

    @Inject
    StorageLocationConfigService storageLocationConfigService;

    @Inject
    StockMovementRepository stockMovementRepository;

    @Inject
    StockBatchMovementRepository stockBatchMovementRepository;

    @Inject
    UserRepository userRepository;

    /**
     * Result of stock update orchestration.
     */
    public record StockUpdateResult(
        int batchesCreated,
        int totalSales,
        List<UUID> newBatchIds,
        UUID fotoMovementId
    ) {}

    /**
     * Process stock update for a completed session.
     *
     * @param sessionId The completed processing session ID
     * @return Result with created batches and detected sales
     */
    @Transactional
    public StockUpdateResult processStockUpdate(UUID sessionId) {
        PhotoProcessingSession session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", sessionId));

        UUID storageLocationId = session.getStorageLocationId();
        UUID userId = session.getUploadedBy();

        if (storageLocationId == null) {
            log.warnf("Session %s has no storage location - skipping stock update", sessionId);
            return new StockUpdateResult(0, 0, List.of(), null);
        }

        if (userId == null) {
            log.warnf("Session %s has no uploaded_by user - skipping stock update", sessionId);
            return new StockUpdateResult(0, 0, List.of(), null);
        }

        // Get active configs for this location
        List<StorageLocationConfig> configs = storageLocationConfigService.getActiveConfigsByLocation(storageLocationId);
        if (configs.isEmpty()) {
            log.warnf("No active configs for storage location %s - skipping stock update", storageLocationId);
            return new StockUpdateResult(0, 0, List.of(), null);
        }

        // Extract count from estimations
        int totalCount = extractCountFromEstimations(sessionId);
        if (totalCount <= 0) {
            log.infof("Session %s has zero count estimation - no stock update needed", sessionId);
            return new StockUpdateResult(0, 0, List.of(), null);
        }

        log.infof("Processing stock update for session %s: location=%s, count=%d, configs=%d",
                sessionId, storageLocationId, totalCount, configs.size());

        // Track all new batches for FOTO movement
        List<StockBatch> newBatches = new ArrayList<>();
        List<SalesInfo> salesInfoList = new ArrayList<>();

        // Process each config
        for (StorageLocationConfig config : configs) {
            // For now, use default state (ACTIVE) - can be enhanced with ML state detection
            ProductState productState = ProductState.ACTIVE;

            // Start new cycle
            CycleResult result = stockBatchService.startNewCycle(
                    storageLocationId,
                    config.getProduct().getId(),
                    productState,
                    totalCount,
                    null, // productSizeId - can be enhanced with size detection
                    config.getPackagingCatalog() != null ? config.getPackagingCatalog().getId() : null,
                    userId,
                    SourceType.IA
            );

            newBatches.add(result.newBatch());
            if (result.salesInfo() != null) {
                salesInfoList.add(result.salesInfo());
            }

            log.infof("Started cycle for config: product=%s, batch=%s, cycle=%d",
                    config.getProduct().getId(),
                    result.newBatch().getBatchCode(),
                    result.newBatch().getCycleNumber());
        }

        // Create FOTO movement linking all new batches
        UUID fotoMovementId = null;
        if (!newBatches.isEmpty()) {
            fotoMovementId = createFotoMovement(session, newBatches, totalCount, userId);
        }

        // Calculate total sales
        int totalSales = salesInfoList.stream()
                .filter(s -> SalesInfo.TYPE_VENTAS.equals(s.type()))
                .mapToInt(SalesInfo::diff)
                .sum();

        if (totalSales > 0) {
            log.infof("Session %s auto-detected %d total sales across %d batches",
                    sessionId, totalSales, salesInfoList.size());
        }

        List<UUID> newBatchIds = newBatches.stream().map(StockBatch::getId).toList();
        return new StockUpdateResult(newBatches.size(), totalSales, newBatchIds, fotoMovementId);
    }

    /**
     * Extract total count from COUNT-type estimations.
     */
    private int extractCountFromEstimations(UUID sessionId) {
        List<Estimation> estimations = estimationRepository.findBySessionId(sessionId);
        return estimations.stream()
                .filter(e -> ESTIMATION_TYPE_COUNT.equalsIgnoreCase(e.getEstimationType()))
                .mapToInt(e -> e.getValue().intValue())
                .findFirst()
                .orElse(0);
    }

    /**
     * Create FOTO movement linking all affected batches.
     */
    private UUID createFotoMovement(PhotoProcessingSession session, List<StockBatch> batches, int quantity, UUID userId) {
        StockMovement movement = new StockMovement();
        movement.setMovementType(MovementType.FOTO);
        movement.setQuantity(quantity);
        movement.setInbound(true);
        movement.setUser(userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId)));
        movement.setSourceType(SourceType.IA);
        movement.setReasonDescription("Photo processing session " + session.getId());
        movement.setProcessingSession(session);
        movement.setPerformedAt(Instant.now());

        stockMovementRepository.persist(movement);

        // Link to all batches
        int order = 1;
        for (StockBatch batch : batches) {
            StockBatchMovement batchMovement = new StockBatchMovement();
            batchMovement.setMovement(movement);
            batchMovement.setBatch(batch);
            batchMovement.setQuantity(BigDecimal.valueOf(quantity));
            batchMovement.setCycleInitiator(true);
            batchMovement.setMovementOrder(order++);

            stockBatchMovementRepository.persist(batchMovement);
        }

        log.infof("Created FOTO movement %s linking %d batches", movement.getId(), batches.size());
        return movement.getId();
    }
}
