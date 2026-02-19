-- V12: Enhance classifications for product/size/state/packaging tracking

-- Add new columns
ALTER TABLE classifications
    ADD COLUMN session_id UUID,
    ADD COLUMN product_id UUID,
    ADD COLUMN product_size_id UUID,
    ADD COLUMN product_state VARCHAR(50),
    ADD COLUMN packaging_catalog_id UUID,
    ADD COLUMN product_conf INTEGER,
    ADD COLUMN product_size_conf INTEGER,
    ADD COLUMN product_state_conf INTEGER,
    ADD COLUMN packaging_conf INTEGER,
    ADD COLUMN model_version VARCHAR(100),
    ADD COLUMN name VARCHAR(255),
    ADD COLUMN description TEXT,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- Migrate image_id to session_id via the images table
UPDATE classifications c
SET session_id = i.session_id
FROM images i
WHERE c.image_id = i.id AND c.session_id IS NULL;

-- Make image_id nullable (deprecated)
ALTER TABLE classifications
    ALTER COLUMN image_id DROP NOT NULL,
    ALTER COLUMN category DROP NOT NULL,
    ALTER COLUMN confidence DROP NOT NULL;

-- Add foreign key constraints
ALTER TABLE classifications
    ADD CONSTRAINT fk_cls_session FOREIGN KEY (session_id)
        REFERENCES photo_processing_sessions(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_cls_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_cls_product_size FOREIGN KEY (product_size_id)
        REFERENCES product_sizes(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_cls_packaging FOREIGN KEY (packaging_catalog_id)
        REFERENCES packaging_catalogs(id) ON DELETE SET NULL;

-- Add indexes
CREATE INDEX idx_cls_session ON classifications(session_id);
CREATE INDEX idx_cls_product ON classifications(product_id);
CREATE INDEX idx_cls_product_size ON classifications(product_size_id);
CREATE INDEX idx_cls_product_state ON classifications(product_state);
CREATE INDEX idx_cls_packaging ON classifications(packaging_catalog_id);

-- Comments
COMMENT ON COLUMN classifications.product_conf IS 'ML confidence score for product classification (0-100 or 0-1000)';
COMMENT ON COLUMN classifications.model_version IS 'Version of the ML model that produced this classification';
