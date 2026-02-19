-- V10: Enhance stock_movements for ML processing and better tracking

-- Add new columns
ALTER TABLE stock_movements
    ADD COLUMN is_inbound BOOLEAN,
    ADD COLUMN user_id UUID,
    ADD COLUMN source_type VARCHAR(20),
    ADD COLUMN reason_description TEXT,
    ADD COLUMN processing_session_id UUID,
    ADD COLUMN parent_movement_id UUID,
    ADD COLUMN unit_price DECIMAL(10,2),
    ADD COLUMN total_price DECIMAL(10,2),
    ADD COLUMN quantity_legacy DECIMAL(12,2);

-- Migrate existing data
UPDATE stock_movements
SET quantity_legacy = quantity,
    is_inbound = CASE
        WHEN movement_type IN ('ENTRADA', 'FOTO', 'MANUAL_INIT', 'PLANTADO') THEN true
        ELSE false
    END,
    user_id = performed_by,
    source_type = 'MANUAL'
WHERE is_inbound IS NULL;

-- Rename old quantity column and create new integer column
ALTER TABLE stock_movements RENAME COLUMN quantity TO quantity_old;
ALTER TABLE stock_movements ADD COLUMN quantity INTEGER;
UPDATE stock_movements SET quantity = CAST(quantity_old AS INTEGER);

-- Make new required columns NOT NULL after migration
ALTER TABLE stock_movements
    ALTER COLUMN is_inbound SET NOT NULL,
    ALTER COLUMN source_type SET NOT NULL,
    ALTER COLUMN quantity SET NOT NULL;

-- Add foreign key constraints
ALTER TABLE stock_movements
    ADD CONSTRAINT fk_sm_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_sm_processing_session FOREIGN KEY (processing_session_id)
        REFERENCES photo_processing_sessions(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_sm_parent_movement FOREIGN KEY (parent_movement_id)
        REFERENCES stock_movements(id) ON DELETE SET NULL;

-- Add indexes
CREATE INDEX idx_sm_user ON stock_movements(user_id);
CREATE INDEX idx_sm_source_type ON stock_movements(source_type);
CREATE INDEX idx_sm_processing_session ON stock_movements(processing_session_id);
CREATE INDEX idx_sm_parent_movement ON stock_movements(parent_movement_id);
CREATE INDEX idx_sm_is_inbound ON stock_movements(is_inbound);

-- Comment
COMMENT ON COLUMN stock_movements.quantity IS 'Signed quantity: positive for inbound, negative for outbound';
COMMENT ON COLUMN stock_movements.source_type IS 'MANUAL or IA (ML-generated)';
