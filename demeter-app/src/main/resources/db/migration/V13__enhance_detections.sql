-- V13: Enhance detections for session-based tracking and geometry

-- Add new columns
ALTER TABLE detections
    ADD COLUMN session_id UUID,
    ADD COLUMN classification_id UUID,
    ADD COLUMN center_x_px INTEGER,
    ADD COLUMN center_y_px INTEGER,
    ADD COLUMN width_px INTEGER,
    ADD COLUMN height_px INTEGER,
    ADD COLUMN is_alive BOOLEAN DEFAULT true,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- Migrate image_id to session_id via the images table
UPDATE detections d
SET session_id = i.session_id
FROM images i
WHERE d.image_id = i.id AND d.session_id IS NULL;

-- Make image_id nullable (deprecated)
ALTER TABLE detections
    ALTER COLUMN image_id DROP NOT NULL;

-- Add foreign key constraints
ALTER TABLE detections
    ADD CONSTRAINT fk_det_session FOREIGN KEY (session_id)
        REFERENCES photo_processing_sessions(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_det_classification FOREIGN KEY (classification_id)
        REFERENCES classifications(id) ON DELETE SET NULL;

-- Add indexes
CREATE INDEX idx_det_session ON detections(session_id);
CREATE INDEX idx_det_classification ON detections(classification_id);
CREATE INDEX idx_det_is_alive ON detections(is_alive) WHERE is_alive = true;

-- Comments
COMMENT ON COLUMN detections.session_id IS 'Processing session that produced this detection';
COMMENT ON COLUMN detections.classification_id IS 'Classification this detection belongs to (optional)';
COMMENT ON COLUMN detections.center_x_px IS 'X coordinate of detection center in pixels';
COMMENT ON COLUMN detections.center_y_px IS 'Y coordinate of detection center in pixels';
COMMENT ON COLUMN detections.width_px IS 'Width of detection bounding box in pixels';
COMMENT ON COLUMN detections.height_px IS 'Height of detection bounding box in pixels';
COMMENT ON COLUMN detections.is_alive IS 'Whether the detected plant appears alive';
