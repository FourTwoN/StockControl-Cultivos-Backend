package com.fortytwo.demeter.inventario.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.empaquetado.model.PackagingCatalog;
import com.fortytwo.demeter.empaquetado.repository.PackagingCatalogRepository;
import com.fortytwo.demeter.inventario.dto.CreateStockBatchRequest;
import com.fortytwo.demeter.inventario.dto.CycleResult;
import com.fortytwo.demeter.inventario.dto.SalesInfo;
import com.fortytwo.demeter.inventario.dto.StockBatchDTO;
import com.fortytwo.demeter.inventario.dto.UpdateStockBatchRequest;
import com.fortytwo.demeter.inventario.model.BatchStatus;
import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.model.SourceType;
import com.fortytwo.demeter.inventario.model.StockBatch;
import com.fortytwo.demeter.inventario.model.StockBatchMovement;
import com.fortytwo.demeter.inventario.model.StockMovement;
import com.fortytwo.demeter.inventario.repository.StockBatchMovementRepository;
import com.fortytwo.demeter.inventario.repository.StockBatchRepository;
import com.fortytwo.demeter.inventario.repository.StockMovementRepository;
import com.fortytwo.demeter.productos.model.Product;
import com.fortytwo.demeter.productos.model.ProductSize;
import com.fortytwo.demeter.productos.model.ProductState;
import com.fortytwo.demeter.productos.repository.ProductRepository;
import com.fortytwo.demeter.productos.repository.ProductSizeRepository;
import com.fortytwo.demeter.ubicaciones.model.StorageLocation;
import com.fortytwo.demeter.ubicaciones.repository.StorageLocationRepository;
import com.fortytwo.demeter.usuarios.model.User;
import com.fortytwo.demeter.usuarios.repository.UserRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StockBatchService {

    private static final Logger log = Logger.getLogger(StockBatchService.class);

    @Inject
    StockBatchRepository stockBatchRepository;

    @Inject
    StockMovementRepository stockMovementRepository;

    @Inject
    StockBatchMovementRepository stockBatchMovementRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    StorageLocationRepository storageLocationRepository;

    @Inject
    ProductSizeRepository productSizeRepository;

    @Inject
    PackagingCatalogRepository packagingCatalogRepository;

    @Inject
    UserRepository userRepository;

    public PagedResponse<StockBatchDTO> findAll(int page, int size, UUID productId, UUID locationId, String status, Boolean activeOnly) {
        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;
        if (productId != null) {
            query.append(" and product.id = ?").append(paramIndex++);
            params.add(productId);
        }
        if (locationId != null) {
            query.append(" and currentStorageLocation.id = ?").append(paramIndex++);
            params.add(locationId);
        }
        if (status != null && !status.isBlank()) {
            query.append(" and status = ?").append(paramIndex++);
            params.add(BatchStatus.valueOf(status));
        }
        if (activeOnly != null && activeOnly) {
            query.append(" and cycleEndDate IS NULL");
        }
        String jpql = query.toString();
        long total = stockBatchRepository.count(jpql, params.toArray());
        var batches = stockBatchRepository.find(jpql + " order by createdAt desc", params.toArray())
                .page(Page.of(page, size)).list();
        return PagedResponse.of(batches.stream().map(StockBatchDTO::from).toList(), page, size, total);
    }

    public StockBatchDTO findById(UUID id) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));
        return StockBatchDTO.from(batch);
    }

    public List<StockBatchDTO> findByProductId(UUID productId) {
        return stockBatchRepository.findByProductId(productId)
                .stream().map(StockBatchDTO::from).toList();
    }

    public List<StockBatchDTO> findByStorageLocationId(UUID storageLocationId) {
        return stockBatchRepository.findByStorageLocationId(storageLocationId)
                .stream().map(StockBatchDTO::from).toList();
    }

    public List<StockBatchDTO> findActiveByStorageLocation(UUID storageLocationId) {
        return stockBatchRepository.findActiveByStorageLocation(storageLocationId)
                .stream().map(StockBatchDTO::from).toList();
    }

    public List<StockBatchDTO> findByStatus(BatchStatus status) {
        return stockBatchRepository.findByStatus(status)
                .stream().map(StockBatchDTO::from).toList();
    }

    @Transactional
    public StockBatchDTO create(CreateStockBatchRequest request) {
        StockBatch batch = new StockBatch();
        batch.setProduct(productRepository.findByIdOptional(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product", request.productId())));
        batch.setBatchCode(request.batchCode());
        batch.setCurrentStorageLocation(storageLocationRepository.findByIdOptional(request.storageLocationId())
                .orElseThrow(() -> new EntityNotFoundException("StorageLocation", request.storageLocationId())));
        batch.setProductState(ProductState.valueOf(request.productState()));

        if (request.productSizeId() != null) {
            batch.setProductSize(productSizeRepository.findByIdOptional(request.productSizeId())
                    .orElseThrow(() -> new EntityNotFoundException("ProductSize", request.productSizeId())));
        }
        if (request.packagingCatalogId() != null) {
            batch.setPackagingCatalog(packagingCatalogRepository.findByIdOptional(request.packagingCatalogId())
                    .orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", request.packagingCatalogId())));
        }

        batch.setCycleNumber(1);
        batch.setCycleStartDate(request.cycleStartDate() != null ? request.cycleStartDate() : Instant.now());
        batch.setQuantityInitial(request.quantity());
        batch.setQuantityCurrent(request.quantity());
        batch.setPlantingDate(request.plantingDate());
        batch.setGerminationDate(request.germinationDate());
        batch.setTransplantDate(request.transplantDate());
        batch.setExpectedReadyDate(request.expectedReadyDate());
        batch.setQualityScore(request.qualityScore());
        batch.setNotes(request.notes());
        batch.setCustomAttributes(request.customAttributes());

        stockBatchRepository.persist(batch);
        return StockBatchDTO.from(batch);
    }

    @Transactional
    public StockBatchDTO update(UUID id, UpdateStockBatchRequest request) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));

        if (request.storageLocationId() != null) {
            batch.setCurrentStorageLocation(storageLocationRepository.findByIdOptional(request.storageLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("StorageLocation", request.storageLocationId())));
        }
        if (request.productState() != null) {
            batch.setProductState(ProductState.valueOf(request.productState()));
        }
        if (request.productSizeId() != null) {
            batch.setProductSize(productSizeRepository.findByIdOptional(request.productSizeId())
                    .orElseThrow(() -> new EntityNotFoundException("ProductSize", request.productSizeId())));
        }
        if (request.packagingCatalogId() != null) {
            batch.setPackagingCatalog(packagingCatalogRepository.findByIdOptional(request.packagingCatalogId())
                    .orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", request.packagingCatalogId())));
        }
        if (request.quantityCurrent() != null) {
            batch.setQuantityCurrent(request.quantityCurrent());
            if (request.quantityCurrent() <= 0) {
                batch.setStatus(BatchStatus.DEPLETED);
            }
        }
        if (request.plantingDate() != null) batch.setPlantingDate(request.plantingDate());
        if (request.germinationDate() != null) batch.setGerminationDate(request.germinationDate());
        if (request.transplantDate() != null) batch.setTransplantDate(request.transplantDate());
        if (request.expectedReadyDate() != null) batch.setExpectedReadyDate(request.expectedReadyDate());
        if (request.qualityScore() != null) batch.setQualityScore(request.qualityScore());
        if (request.notes() != null) batch.setNotes(request.notes());
        if (request.customAttributes() != null) batch.setCustomAttributes(request.customAttributes());
        if (request.status() != null) batch.setStatus(BatchStatus.valueOf(request.status()));

        return StockBatchDTO.from(batch);
    }

    @Transactional
    public void updateQuantity(UUID id, Integer newQuantity) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));
        batch.setQuantityCurrent(newQuantity);

        if (newQuantity <= 0) {
            batch.setStatus(BatchStatus.DEPLETED);
        }
    }

    @Transactional
    public void closeBatch(UUID id) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));
        batch.setCycleEndDate(Instant.now());
    }

    @Transactional
    public void delete(UUID id) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));
        stockBatchRepository.delete(batch);
    }

    /**
     * Start a new cycle for a stock batch.
     *
     * <p>This is the core business logic for ML-driven stock updates:
     * <ol>
     *   <li>Find active batch for the location/product/state/size/packaging combination</li>
     *   <li>If found and quantity decreased → create VENTA movement (auto-sales detection)</li>
     *   <li>If found and quantity increased → log warning (anomaly, unregistered planting)</li>
     *   <li>Close the old batch (set cycleEndDate)</li>
     *   <li>Create new batch with incremented cycle number</li>
     * </ol>
     *
     * @param locationId Storage location ID
     * @param productId Product ID
     * @param productState Product state (e.g., SEMILLA, PLANTULA)
     * @param newQuantity New quantity detected by ML
     * @param productSizeId Product size ID (nullable)
     * @param packagingCatalogId Packaging catalog ID (nullable)
     * @param userId User performing the action
     * @param sourceType Source of this cycle (MANUAL or IA)
     * @return CycleResult with new batch and optional sales info
     */
    @Transactional
    public CycleResult startNewCycle(
            UUID locationId,
            UUID productId,
            ProductState productState,
            int newQuantity,
            UUID productSizeId,
            UUID packagingCatalogId,
            UUID userId,
            SourceType sourceType
    ) {
        // Load entities
        StorageLocation location = storageLocationRepository.findByIdOptional(locationId)
                .orElseThrow(() -> new EntityNotFoundException("StorageLocation", locationId));
        Product product = productRepository.findByIdOptional(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        ProductSize productSize = null;
        if (productSizeId != null) {
            productSize = productSizeRepository.findByIdOptional(productSizeId)
                    .orElseThrow(() -> new EntityNotFoundException("ProductSize", productSizeId));
        }

        PackagingCatalog packagingCatalog = null;
        if (packagingCatalogId != null) {
            packagingCatalog = packagingCatalogRepository.findByIdOptional(packagingCatalogId)
                    .orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", packagingCatalogId));
        }

        // Find active batch
        Optional<StockBatch> activeBatchOpt = stockBatchRepository.findActiveBatch(
                locationId, productId, productState, productSizeId, packagingCatalogId);

        SalesInfo salesInfo = null;
        int nextCycleNumber = 1;

        if (activeBatchOpt.isPresent()) {
            StockBatch activeBatch = activeBatchOpt.get();
            int previousQty = activeBatch.getQuantityCurrent();
            nextCycleNumber = activeBatch.getCycleNumber() + 1;

            // Compare quantities
            if (newQuantity < previousQty) {
                // Sales detected - create VENTA movement
                int salesQty = previousQty - newQuantity;
                createVentaMovement(activeBatch, salesQty, user, sourceType);
                salesInfo = SalesInfo.sales(previousQty, newQuantity, true);
                log.infof("Auto-sales detected: batch=%s, sold=%d, previous=%d, new=%d",
                        activeBatch.getBatchCode(), salesQty, previousQty, newQuantity);
            } else if (newQuantity > previousQty) {
                // Anomaly - more plants than before (unregistered planting)
                salesInfo = SalesInfo.unregisteredPlanting(previousQty, newQuantity);
                log.warnf("Anomaly: more items than before. batch=%s, previous=%d, new=%d. Possible unregistered planting.",
                        activeBatch.getBatchCode(), previousQty, newQuantity);
            } else {
                // No change
                salesInfo = SalesInfo.noChange(previousQty);
            }

            // Close old batch
            activeBatch.setCycleEndDate(Instant.now());
            log.infof("Closed batch %s cycle %d", activeBatch.getBatchCode(), activeBatch.getCycleNumber());
        } else {
            // First cycle - get latest cycle number from history
            Integer latestCycle = stockBatchRepository.getLatestCycleNumber(
                    locationId, productId, productState, productSizeId, packagingCatalogId);
            nextCycleNumber = latestCycle + 1;
        }

        // Create new batch
        StockBatch newBatch = new StockBatch();
        newBatch.setProduct(product);
        newBatch.setBatchCode(generateBatchCode(product, location, nextCycleNumber));
        newBatch.setCurrentStorageLocation(location);
        newBatch.setProductState(productState);
        newBatch.setProductSize(productSize);
        newBatch.setPackagingCatalog(packagingCatalog);
        newBatch.setCycleNumber(nextCycleNumber);
        newBatch.setCycleStartDate(Instant.now());
        newBatch.setQuantityInitial(newQuantity);
        newBatch.setQuantityCurrent(newQuantity);
        newBatch.setStatus(newQuantity > 0 ? BatchStatus.ACTIVE : BatchStatus.DEPLETED);

        stockBatchRepository.persist(newBatch);
        log.infof("Created new batch %s cycle %d with quantity %d",
                newBatch.getBatchCode(), nextCycleNumber, newQuantity);

        return new CycleResult(newBatch, salesInfo);
    }

    private void createVentaMovement(StockBatch batch, int quantity, User user, SourceType sourceType) {
        StockMovement movement = new StockMovement();
        movement.setMovementType(MovementType.VENTA);
        movement.setQuantity(quantity);
        movement.setInbound(false);
        movement.setUser(user);
        movement.setSourceType(sourceType);
        movement.setReasonDescription("Auto-detected sales from photo cycle comparison");
        movement.setPerformedAt(Instant.now());

        stockMovementRepository.persist(movement);

        // Create batch-movement link
        StockBatchMovement batchMovement = new StockBatchMovement();
        batchMovement.setMovement(movement);
        batchMovement.setBatch(batch);
        batchMovement.setQuantity(new java.math.BigDecimal(quantity));

        stockBatchMovementRepository.persist(batchMovement);
    }

    private String generateBatchCode(Product product, StorageLocation location, int cycleNumber) {
        String productCode = product.getSku() != null ? product.getSku() : product.getId().toString().substring(0, 8);
        String locationCode = location.getName() != null ?
                location.getName().substring(0, Math.min(4, location.getName().length())).toUpperCase() :
                location.getId().toString().substring(0, 4);
        return String.format("%s-%s-C%03d", productCode, locationCode, cycleNumber);
    }
}
