-- V14: Enhance estimations for classification linking and area/count tracking

-- Add new columns
ALTER TABLE estimations
    ADD COLUMN classification_id UUID,
    ADD COLUMN vegetation_polygon JSONB,
    ADD COLUMN detected_area_cm2 DECIMAL(10,2),
    ADD COLUMN estimated_count INTEGER,
    ADD COLUMN calculation_method VARCHAR(100),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- Add foreign key constraint
ALTER TABLE estimations
    ADD CONSTRAINT fk_est_classification FOREIGN KEY (classification_id)
        REFERENCES classifications(id) ON DELETE SET NULL;

-- Add indexes
CREATE INDEX idx_est_classification ON estimations(classification_id);
CREATE INDEX idx_est_type ON estimations(estimation_type);

-- Comments
COMMENT ON COLUMN estimations.classification_id IS 'Classification this estimation applies to (optional)';
COMMENT ON COLUMN estimations.vegetation_polygon IS 'GeoJSON polygon of detected vegetation area';
COMMENT ON COLUMN estimations.detected_area_cm2 IS 'Calculated area of detected vegetation in cm²';
COMMENT ON COLUMN estimations.estimated_count IS 'Estimated count of items (plants, fruits, etc.)';
COMMENT ON COLUMN estimations.calculation_method IS 'Method used for estimation (e.g., PIXEL_COUNT, DETECTION_COUNT)';
