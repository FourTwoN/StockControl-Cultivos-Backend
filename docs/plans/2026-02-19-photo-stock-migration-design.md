# Photo → Stock Update Migration Design

**Date:** 2026-02-19
**Status:** ✅ Completed (2026-02-19)
**Author:** Claude + Franco

## Overview

Migration of the photo processing → stock update flow from the old Python/FastAPI backend (`DemeterAI-back`) to the new Quarkus backend (`StockControl-Backend`).

When ML Worker processes a photo and sends results via callback, the system must:
1. Persist ML results (detections, classifications, estimations)
2. Create/update stock batches with cycle management
3. Calculate automatic sales when quantity decreases between cycles
4. Create audit trail via stock movements

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| `storage_bins` table | NOT migrated | Per user requirement - only `storage_bin_types` kept |
| Cycle management | Full implementation | Auto-sales detection, cycle tracking |
| Classification → StockMovement link | Via Session (indirect) | Cleaner model, no redundant FK |
| Detection → Image link | Removed | Old system didn't have it, Detection → Session only |
| Movement ↔ Batch relationship | M:N via junction table | One foto movement → N batches |
| Orchestration order | demeter-fotos first, then demeter-inventario | Persist ML results before inventory impact |

---

## Entity Layer (Phase 1)

### New Entity: StorageLocationConfig

**Module:** demeter-ubicaciones

```java
@Entity
@Table(name = "storage_location_config")
public class StorageLocationConfig extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "storage_location_id", nullable = false)
    StorageLocation storageLocation;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @ManyToOne
    @JoinColumn(name = "packaging_catalog_id")
    PackagingCatalog packagingCatalog;  // nullable

    boolean active = true;

    String notes;
}
```

**Constraint:** `UNIQUE(storage_location_id, product_id, packaging_catalog_id)`

---

### Enhanced Entity: StockBatch

**Module:** demeter-inventario

```java
@Entity
@Table(name = "stock_batches")
public class StockBatch extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(name = "batch_code", nullable = false, length = 100)
    String batchCode;

    @ManyToOne
    @JoinColumn(name = "current_storage_location_id", nullable = false)
    StorageLocation currentStorageLocation;

    @ManyToOne
    @JoinColumn(name = "product_state_id", nullable = false)
    ProductState productState;

    @ManyToOne
    @JoinColumn(name = "product_size_id")
    ProductSize productSize;  // nullable

    @ManyToOne
    @JoinColumn(name = "packaging_catalog_id")
    PackagingCatalog packagingCatalog;  // nullable

    // Cycle tracking
    @Column(name = "cycle_number", nullable = false)
    Integer cycleNumber = 1;

    @Column(name = "cycle_start_date", nullable = false)
    Instant cycleStartDate;

    @Column(name = "cycle_end_date")
    Instant cycleEndDate;  // null = active batch

    // Quantity tracking
    @Column(name = "quantity_initial", nullable = false)
    Integer quantityInitial;

    @Column(name = "quantity_current", nullable = false)
    Integer quantityCurrent;

    // Growth tracking (optional)
    @Column(name = "planting_date")
    LocalDate plantingDate;

    @Column(name = "germination_date")
    LocalDate germinationDate;

    @Column(name = "transplant_date")
    LocalDate transplantDate;

    @Column(name = "expected_ready_date")
    LocalDate expectedReadyDate;

    // Quality
    @Column(name = "quality_score", precision = 3, scale = 2)
    BigDecimal qualityScore;  // 0.00-5.00

    String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes", columnDefinition = "jsonb")
    Map<String, Object> customAttributes;
}
```

**Key constraint:** Only ONE active batch per `(location, product, state, size, packaging)` - enforced by `cycleEndDate IS NULL`.

---

### New Enum: MovementType

**Module:** demeter-inventario

```java
public enum MovementType {
    FOTO,                  // ML photo-based stock initialization
    MANUAL_INIT,           // Manual stock initialization
    MUERTE,                // Plant death (outbound/loss)
    PLANTADO,              // New planting (inbound)
    MOVIMIENTO,            // Pure location change
    TRASPLANTE,            // Config change within same location
    MOVIMIENTO_TRASPLANTE, // Location + config change
    AJUSTE,                // Manual adjustment (+/-)
    VENTA                  // Sale (outbound, auto-calculated)
}
```

---

### New Enum: SourceType

**Module:** demeter-inventario

```java
public enum SourceType {
    MANUAL,  // User-initiated
    IA       // ML-generated
}
```

---

### New/Enhanced Entity: StockMovement

**Module:** demeter-inventario

```java
@Entity
@Table(name = "stock_movements")
public class StockMovement extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    MovementType movementType;

    @Column(nullable = false)
    Integer quantity;  // signed: + inbound, - outbound

    @Column(name = "is_inbound", nullable = false)
    boolean isInbound;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    SourceType sourceType;

    @Column(name = "reason_description")
    String reasonDescription;

    @ManyToOne
    @JoinColumn(name = "processing_session_id")
    PhotoProcessingSession processingSession;  // nullable, for FOTO type

    @ManyToOne
    @JoinColumn(name = "parent_movement_id")
    StockMovement parentMovement;  // nullable, self-referencing

    // COGS tracking (optional)
    @Column(name = "unit_price", precision = 10, scale = 2)
    BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    BigDecimal totalPrice;
}
```

---

### Entity: StockBatchMovement (Junction Table)

**Module:** demeter-inventario

```java
@Entity
@Table(name = "stock_batch_movements")
public class StockBatchMovement extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "movement_id", nullable = false)
    StockMovement movement;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    StockBatch batch;

    @Column(name = "is_cycle_initiator")
    boolean isCycleInitiator = false;

    @Column(name = "movement_order")
    Integer movementOrder;  // nullable
}
```

---

### Enhanced Entity: Classification

**Module:** demeter-fotos

```java
@Entity
@Table(name = "classifications")
public class Classification extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    PhotoProcessingSession session;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

    @ManyToOne
    @JoinColumn(name = "product_size_id")
    ProductSize productSize;

    @ManyToOne
    @JoinColumn(name = "product_state_id")
    ProductState productState;

    @ManyToOne
    @JoinColumn(name = "packaging_catalog_id")
    PackagingCatalog packagingCatalog;

    @Column(name = "product_conf")
    Integer productConf;

    @Column(name = "product_size_conf")
    Integer productSizeConf;

    @Column(name = "product_state_conf")
    Integer productStateConf;

    @Column(name = "packaging_conf")
    Integer packagingConf;

    @Column(name = "model_version")
    String modelVersion;

    String name;
    String description;
}
```

---

### Enhanced Entity: Detection

**Module:** demeter-fotos

```java
@Entity
@Table(name = "detections")
public class Detection extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    PhotoProcessingSession session;

    @ManyToOne
    @JoinColumn(name = "classification_id")
    Classification classification;

    String label;

    @Column(precision = 5, scale = 4)
    BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bounding_box", columnDefinition = "jsonb")
    Map<String, Object> boundingBox;  // {x1, y1, x2, y2}

    @Column(name = "center_x_px")
    Integer centerXPx;

    @Column(name = "center_y_px")
    Integer centerYPx;

    @Column(name = "width_px")
    Integer widthPx;

    @Column(name = "height_px")
    Integer heightPx;

    @Column(name = "is_alive")
    boolean isAlive = true;
}
```

---

### Enhanced Entity: Estimation

**Module:** demeter-fotos

```java
@Entity
@Table(name = "estimations")
public class Estimation extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    PhotoProcessingSession session;

    @ManyToOne
    @JoinColumn(name = "classification_id")
    Classification classification;

    @Column(name = "estimation_type")
    String estimationType;

    @Column(precision = 12, scale = 2)
    BigDecimal value;

    String unit;

    @Column(precision = 5, scale = 4)
    BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vegetation_polygon", columnDefinition = "jsonb")
    Map<String, Object> vegetationPolygon;

    @Column(name = "detected_area_cm2", precision = 10, scale = 2)
    BigDecimal detectedAreaCm2;

    @Column(name = "estimated_count")
    Integer estimatedCount;

    @Column(name = "calculation_method")
    String calculationMethod;
}
```

---

## Service Layer (Phase 2)

### Service Architecture

```
demeter-ubicaciones/
└── service/
    └── StorageLocationConfigService.java

demeter-inventario/
└── service/
    ├── StockBatchService.java           # with startNewCycle()
    ├── StockMovementService.java
    └── StockBatchMovementService.java

demeter-fotos/
└── service/
    ├── ProcessingResultService.java     # orchestrator
    ├── ClassificationService.java
    ├── DetectionService.java
    └── EstimationService.java
```

### StockBatchService.startNewCycle()

Core business logic for cycle management:

```java
public record CycleResult(
    StockBatch newBatch,
    SalesInfo salesInfo  // nullable
) {}

public record SalesInfo(
    int previousQty,
    int newQty,
    int diff,
    String type,  // "ventas" | "plantado_no_registrado" | "sin_cambio"
    boolean movementCreated
) {}

@Transactional
public CycleResult startNewCycle(
    UUID locationId,
    UUID productId,
    UUID stateId,
    int newQuantity,
    UUID sizeId,        // nullable
    UUID packagingId,   // nullable
    UUID userId
) {
    // 1. Find active batch (cycleEndDate IS NULL)
    // 2. If found:
    //    a. Compare newQuantity vs batch.quantityCurrent
    //    b. If LESS → create VENTA movement
    //    c. If MORE → log warning (anomaly)
    //    d. Close old batch (cycleEndDate = now)
    // 3. Create new batch with cycleNumber++
    // 4. Return CycleResult
}
```

---

## Callback Endpoint Wiring (Phase 3)

### Orchestration Flow

```
ProcessingResultService.processResults()
    │
    ├──▶ Validate Session exists
    │
    ├──▶ StorageLocationConfigService.getActiveConfigsByLocation()
    │
    ├──▶ Count items by size from estimations
    │
    │   ════════════════════════════════════════════════════
    │   PHASE A: demeter-fotos tables (ML results)
    │   ════════════════════════════════════════════════════
    │
    ├──▶ ClassificationService.create() [per size group]
    │         └── INSERT: classifications
    │
    ├──▶ DetectionService.create() [per detection]
    │         └── INSERT: detections
    │
    ├──▶ EstimationService.create() [per estimation]
    │         └── INSERT: estimations
    │
    ├──▶ Update session status
    │         └── UPDATE: photo_processing_sessions
    │
    │   ════════════════════════════════════════════════════
    │   PHASE B: demeter-inventario tables (stock impact)
    │   ════════════════════════════════════════════════════
    │
    ├──▶ StockBatchService.startNewCycle() [per config × size]
    │         ├── Find active batch
    │         ├── If qty decreased → createVentaMovement()
    │         │         └── INSERT: stock_movements (type=VENTA)
    │         ├── Close old batch
    │         └── INSERT: stock_batches (new cycle)
    │
    └──▶ StockMovementService.createPhotoMovement()
              ├── INSERT: stock_movements (type=FOTO)
              └── INSERT: stock_batch_movements (M:N links)
```

---

## Implementation Checklist

### Phase 1: Entities & Repositories ✅

- [x] 1.1 Create `StorageLocationConfig` entity (demeter-ubicaciones)
- [x] 1.2 Create `StorageLocationConfigRepository` (demeter-ubicaciones)
- [x] 1.3 Enhance `StockBatch` with cycle fields (demeter-inventario)
- [x] 1.4 Create `MovementType` enum (demeter-inventario)
- [x] 1.5 Create `SourceType` enum (demeter-inventario)
- [x] 1.6 Create/enhance `StockMovement` entity (demeter-inventario)
- [x] 1.7 Verify/enhance `StockBatchMovement` entity (demeter-inventario)
- [x] 1.8 Create repositories for inventory entities (demeter-inventario)
- [x] 1.9 Enhance `Classification` with product FKs (demeter-fotos)
- [x] 1.10 Enhance `Detection` with session + classification FKs (demeter-fotos)
- [x] 1.11 Enhance `Estimation` with session + classification FKs (demeter-fotos)

### Phase 2: Services & Business Logic ✅

- [x] 2.1 Create `StorageLocationConfigService` (demeter-ubicaciones)
- [x] 2.2 Create `StockBatchService` with `startNewCycle()` (demeter-inventario)
- [x] 2.3 Create `StockMovementService` (demeter-inventario)
- [x] 2.4 Create `StockBatchMovementService` (demeter-inventario)
- [x] 2.5 Create/enhance `ClassificationService` (demeter-fotos)
- [x] 2.6 Create/enhance `DetectionService` (demeter-fotos)
- [x] 2.7 Create/enhance `EstimationService` (demeter-fotos)

### Phase 3: Wire Callback Endpoint ✅

- [x] 3.1 Create `StockUpdateOrchestrator` in demeter-app (bridges modules)
- [x] 3.2 Update DTOs if needed (demeter-fotos)
- [ ] 3.3 Add integration tests (demeter-app) — *optional, deferred*

### Flyway Migrations Created

| Migration | Purpose |
|-----------|---------|
| V8 | storage_location_configs table |
| V9 | stock_batches cycle fields |
| V10 | stock_movements enhancements |
| V11 | stock_batch_movements enhancements |
| V12 | classifications product FKs |
| V13 | detections session/geometry |
| V14 | estimations classification/area |
| V15 | sessions storage_location_id |

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| No active config for location | Throw `BusinessException` |
| Session not found | Throw `EntityNotFoundException` |
| Zero detections/estimations | Skip batch creation |
| Duplicate callback | Idempotency check via session status |
| No previous batch | First cycle - cycleNumber=1, no auto-sales |
| More plants than before | Log warning, still create new cycle |
| Transaction failure | `@Transactional` rollback |

---

## Testing Strategy

**Phase 1:** Entity validation tests, repository tests with Testcontainers

**Phase 2:** Service unit tests, especially `startNewCycle()` scenarios:
- First cycle (no previous batch)
- Same quantity (no sales)
- Less quantity (auto-sales)
- More quantity (anomaly)

**Phase 3:** Integration tests with mock ML Worker payload
