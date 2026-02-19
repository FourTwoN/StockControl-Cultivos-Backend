-- V8: Create storage_location_configs table
-- Links storage locations to products (and optionally packaging) for ML processing

CREATE TABLE storage_location_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    storage_location_id UUID NOT NULL,
    product_id UUID NOT NULL,
    packaging_catalog_id UUID,
    active BOOLEAN NOT NULL DEFAULT true,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_slc_storage_location FOREIGN KEY (storage_location_id)
        REFERENCES storage_locations(id) ON DELETE CASCADE,
    CONSTRAINT fk_slc_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_slc_packaging_catalog FOREIGN KEY (packaging_catalog_id)
        REFERENCES packaging_catalogs(id) ON DELETE SET NULL,
    CONSTRAINT uk_storage_location_config
        UNIQUE (storage_location_id, product_id, packaging_catalog_id)
);

-- Index for common lookups
CREATE INDEX idx_slc_storage_location ON storage_location_configs(storage_location_id);
CREATE INDEX idx_slc_product ON storage_location_configs(product_id);
CREATE INDEX idx_slc_active ON storage_location_configs(active) WHERE active = true;
CREATE INDEX idx_slc_tenant ON storage_location_configs(tenant_id);

-- Enable RLS
ALTER TABLE storage_location_configs ENABLE ROW LEVEL SECURITY;

-- RLS policy using current_setting for tenant isolation
CREATE POLICY storage_location_configs_tenant_isolation ON storage_location_configs
    USING (tenant_id = current_setting('app.current_tenant', true));
