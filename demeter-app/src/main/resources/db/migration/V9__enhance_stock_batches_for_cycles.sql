-- V9: Enhance stock_batches for cycle tracking and ML processing
-- Adds cycle management fields, storage location FK, product state/size/packaging FKs

-- Add new columns for cycle tracking
ALTER TABLE stock_batches
    ADD COLUMN current_storage_location_id UUID,
    ADD COLUMN product_state VARCHAR(50),
    ADD COLUMN product_size_id UUID,
    ADD COLUMN packaging_catalog_id UUID,
    ADD COLUMN cycle_number INTEGER DEFAULT 1,
    ADD COLUMN cycle_start_date TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cycle_end_date TIMESTAMP WITH TIME ZONE,
    ADD COLUMN quantity_initial INTEGER,
    ADD COLUMN quantity_current INTEGER,
    ADD COLUMN planting_date DATE,
    ADD COLUMN germination_date DATE,
    ADD COLUMN transplant_date DATE,
    ADD COLUMN expected_ready_date DATE,
    ADD COLUMN quality_score DECIMAL(3,2),
    ADD COLUMN notes TEXT;

-- Make old columns nullable for backward compatibility
ALTER TABLE stock_batches
    ALTER COLUMN quantity DROP NOT NULL,
    ALTER COLUMN entry_date DROP NOT NULL;

-- Add foreign key constraints
ALTER TABLE stock_batches
    ADD CONSTRAINT fk_sb_storage_location FOREIGN KEY (current_storage_location_id)
        REFERENCES storage_locations(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_sb_product_size FOREIGN KEY (product_size_id)
        REFERENCES product_sizes(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_sb_packaging_catalog FOREIGN KEY (packaging_catalog_id)
        REFERENCES packaging_catalogs(id) ON DELETE SET NULL;

-- Migrate existing data: convert old quantity to new integer fields
UPDATE stock_batches
SET quantity_initial = COALESCE(CAST(quantity AS INTEGER), 0),
    quantity_current = COALESCE(CAST(quantity AS INTEGER), 0),
    cycle_start_date = COALESCE(entry_date, created_at),
    cycle_number = 1,
    product_state = 'ACTIVE'
WHERE quantity_initial IS NULL;

-- Now make new required fields NOT NULL after data migration
ALTER TABLE stock_batches
    ALTER COLUMN cycle_number SET NOT NULL,
    ALTER COLUMN cycle_start_date SET NOT NULL,
    ALTER COLUMN quantity_initial SET NOT NULL,
    ALTER COLUMN quantity_current SET NOT NULL,
    ALTER COLUMN product_state SET NOT NULL;

-- Add indexes for new columns
CREATE INDEX idx_sb_storage_location ON stock_batches(current_storage_location_id);
CREATE INDEX idx_sb_product_state ON stock_batches(product_state);
CREATE INDEX idx_sb_product_size ON stock_batches(product_size_id);
CREATE INDEX idx_sb_packaging ON stock_batches(packaging_catalog_id);
CREATE INDEX idx_sb_cycle_active ON stock_batches(current_storage_location_id, product_id, product_state, product_size_id, packaging_catalog_id)
    WHERE cycle_end_date IS NULL;

-- Comment for documentation
COMMENT ON COLUMN stock_batches.cycle_end_date IS 'NULL indicates active batch; non-null means batch is closed';
COMMENT ON COLUMN stock_batches.product_state IS 'Plant/catalog state enum: ACTIVE, INACTIVE, DISCONTINUED';
