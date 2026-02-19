package com.fortytwo.demeter.inventario.model;

import com.fortytwo.demeter.common.model.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Junction table linking movements to affected batches.
 * One movement can affect multiple batches (M:N relationship).
 */
@Entity
@Table(name = "stock_batch_movements")
public class StockBatchMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movement_id", nullable = false)
    private StockMovement movement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private StockBatch batch;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "is_cycle_initiator")
    private boolean isCycleInitiator = false;

    @Column(name = "movement_order")
    private Integer movementOrder;

    // Getters
    public StockMovement getMovement() { return movement; }
    public StockBatch getBatch() { return batch; }
    public BigDecimal getQuantity() { return quantity; }
    public boolean isCycleInitiator() { return isCycleInitiator; }
    public Integer getMovementOrder() { return movementOrder; }

    // Setters
    public void setMovement(StockMovement movement) { this.movement = movement; }
    public void setBatch(StockBatch batch) { this.batch = batch; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setCycleInitiator(boolean cycleInitiator) { isCycleInitiator = cycleInitiator; }
    public void setMovementOrder(Integer movementOrder) { this.movementOrder = movementOrder; }
}
