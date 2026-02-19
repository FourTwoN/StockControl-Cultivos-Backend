package com.fortytwo.demeter.inventario.model;

import com.fortytwo.demeter.common.model.BaseEntity;
import com.fortytwo.demeter.fotos.model.PhotoProcessingSession;
import com.fortytwo.demeter.usuarios.model.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a stock movement event (inbound, outbound, transfer, adjustment).
 * Links to affected batches via StockBatchMovement junction table.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;  // Signed: + inbound, - outbound

    @Column(name = "is_inbound", nullable = false)
    private boolean isInbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "reason_description", columnDefinition = "TEXT")
    private String reasonDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processing_session_id")
    private PhotoProcessingSession processingSession;  // nullable, for FOTO type

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_movement_id")
    private StockMovement parentMovement;  // nullable, self-referencing for related movements

    // COGS tracking (optional)
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @OneToMany(mappedBy = "movement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockBatchMovement> batchMovements = new ArrayList<>();

    // Legacy fields (deprecated - kept for backward compatibility)
    @Deprecated
    @Column(length = 50)
    private String unit;

    @Deprecated
    @Column(name = "reference_id")
    private UUID referenceId;

    @Deprecated
    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Deprecated
    @Column(name = "performed_by")
    private UUID performedBy;

    // Getters
    public MovementType getMovementType() { return movementType; }
    public Integer getQuantity() { return quantity; }
    public boolean isInbound() { return isInbound; }
    public User getUser() { return user; }
    public SourceType getSourceType() { return sourceType; }
    public String getReasonDescription() { return reasonDescription; }
    public PhotoProcessingSession getProcessingSession() { return processingSession; }
    public StockMovement getParentMovement() { return parentMovement; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public Instant getPerformedAt() { return performedAt; }
    public List<StockBatchMovement> getBatchMovements() { return batchMovements; }

    // Legacy getters
    @Deprecated public String getUnit() { return unit; }
    @Deprecated public UUID getReferenceId() { return referenceId; }
    @Deprecated public String getReferenceType() { return referenceType; }
    @Deprecated public String getNotes() { return notes; }
    @Deprecated public UUID getPerformedBy() { return performedBy; }

    // Setters
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setInbound(boolean inbound) { isInbound = inbound; }
    public void setUser(User user) { this.user = user; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    public void setProcessingSession(PhotoProcessingSession processingSession) { this.processingSession = processingSession; }
    public void setParentMovement(StockMovement parentMovement) { this.parentMovement = parentMovement; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }

    // Legacy setters
    @Deprecated public void setUnit(String unit) { this.unit = unit; }
    @Deprecated public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }
    @Deprecated public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    @Deprecated public void setNotes(String notes) { this.notes = notes; }
    @Deprecated public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }
}
