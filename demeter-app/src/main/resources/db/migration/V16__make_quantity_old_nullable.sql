-- V16: Make quantity_old nullable for new movements
-- The quantity_old column is a legacy field from the original decimal quantity column.
-- New movements don't need this field - they use the new integer quantity column.

ALTER TABLE stock_movements
    ALTER COLUMN quantity_old DROP NOT NULL;

COMMENT ON COLUMN stock_movements.quantity_old IS 'Legacy decimal quantity column - nullable for new movements';
