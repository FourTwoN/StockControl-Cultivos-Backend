-- V11: Enhance stock_batch_movements for cycle tracking

-- Add tenant_id and new fields
ALTER TABLE stock_batch_movements
    ADD COLUMN tenant_id VARCHAR(255),
    ADD COLUMN is_cycle_initiator BOOLEAN DEFAULT false,
    ADD COLUMN movement_order INTEGER,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- Migrate tenant_id from the related batch
UPDATE stock_batch_movements sbm
SET tenant_id = sb.tenant_id
FROM stock_batches sb
WHERE sbm.batch_id = sb.id AND sbm.tenant_id IS NULL;

-- Make tenant_id NOT NULL after migration
ALTER TABLE stock_batch_movements
    ALTER COLUMN tenant_id SET NOT NULL;

-- Add indexes
CREATE INDEX idx_sbm_tenant ON stock_batch_movements(tenant_id);
CREATE INDEX idx_sbm_movement ON stock_batch_movements(movement_id);
CREATE INDEX idx_sbm_batch ON stock_batch_movements(batch_id);
CREATE INDEX idx_sbm_cycle_initiator ON stock_batch_movements(is_cycle_initiator) WHERE is_cycle_initiator = true;

-- Enable RLS
ALTER TABLE stock_batch_movements ENABLE ROW LEVEL SECURITY;

-- RLS policy
CREATE POLICY stock_batch_movements_tenant_isolation ON stock_batch_movements
    USING (tenant_id = current_setting('app.current_tenant', true));

-- Comments
COMMENT ON COLUMN stock_batch_movements.is_cycle_initiator IS 'True if this movement started a new cycle for the batch';
COMMENT ON COLUMN stock_batch_movements.movement_order IS 'Order of this batch within a multi-batch movement';
