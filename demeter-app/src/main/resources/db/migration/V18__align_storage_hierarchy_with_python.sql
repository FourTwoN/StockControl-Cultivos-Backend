-- V18: Align storage hierarchy tables with Python model definitions
-- Adds missing columns: code, position, active

-- =============================================
-- WAREHOUSES: Add code column
-- =============================================
ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- Generate unique codes for existing warehouses
UPDATE warehouses
SET code = UPPER(CONCAT('WH-', SUBSTRING(id::text, 1, 8)))
WHERE code IS NULL;

-- Make code NOT NULL and add unique constraint
ALTER TABLE warehouses
    ALTER COLUMN code SET NOT NULL;

-- Add unique index on code (if not exists)
CREATE UNIQUE INDEX IF NOT EXISTS idx_warehouses_code_unique
    ON warehouses(tenant_id, code);

-- =============================================
-- STORAGE_AREAS: Add code, position, active columns
-- =============================================
ALTER TABLE storage_areas
    ADD COLUMN IF NOT EXISTS code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS position VARCHAR(1),
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT true;

-- Generate unique codes for existing areas
UPDATE storage_areas
SET code = UPPER(CONCAT('AREA-', SUBSTRING(id::text, 1, 8)))
WHERE code IS NULL;

-- Set active=true for existing records where deleted_at IS NULL
UPDATE storage_areas
SET active = (deleted_at IS NULL)
WHERE active IS NULL;

-- Make code and active NOT NULL
ALTER TABLE storage_areas
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN active SET NOT NULL,
    ALTER COLUMN active SET DEFAULT true;

-- Add unique index on code
CREATE UNIQUE INDEX IF NOT EXISTS idx_storage_areas_code_unique
    ON storage_areas(tenant_id, code);

-- Add position enum constraint (N/S/E/W/C or NULL)
ALTER TABLE storage_areas
    ADD CONSTRAINT ck_storage_areas_position
    CHECK (position IS NULL OR position IN ('N', 'S', 'E', 'W', 'C'));

-- =============================================
-- STORAGE_LOCATIONS: Add code, active columns
-- =============================================
ALTER TABLE storage_locations
    ADD COLUMN IF NOT EXISTS code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT true;

-- Generate unique codes for existing locations
UPDATE storage_locations
SET code = UPPER(CONCAT('LOC-', SUBSTRING(id::text, 1, 8)))
WHERE code IS NULL;

-- Set active=true for existing records where deleted_at IS NULL
UPDATE storage_locations
SET active = (deleted_at IS NULL)
WHERE active IS NULL;

-- Make code and active NOT NULL
ALTER TABLE storage_locations
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN active SET NOT NULL,
    ALTER COLUMN active SET DEFAULT true;

-- Add unique index on code
CREATE UNIQUE INDEX IF NOT EXISTS idx_storage_locations_code_unique
    ON storage_locations(tenant_id, code);

-- =============================================
-- Add indexes for active columns (soft delete queries)
-- =============================================
CREATE INDEX IF NOT EXISTS idx_storage_areas_active ON storage_areas(active);
CREATE INDEX IF NOT EXISTS idx_storage_locations_active ON storage_locations(active);
