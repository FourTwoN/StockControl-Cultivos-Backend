-- V15: Add storage_location_id to photo_processing_sessions for stock update flow

ALTER TABLE photo_processing_sessions
    ADD COLUMN storage_location_id UUID;

-- Add foreign key constraint
ALTER TABLE photo_processing_sessions
    ADD CONSTRAINT fk_session_storage_location FOREIGN KEY (storage_location_id)
        REFERENCES storage_locations(id) ON DELETE SET NULL;

-- Add index
CREATE INDEX idx_session_storage_location ON photo_processing_sessions(storage_location_id);

-- Comment
COMMENT ON COLUMN photo_processing_sessions.storage_location_id IS 'Storage location where photos were taken (for stock update flow)';
