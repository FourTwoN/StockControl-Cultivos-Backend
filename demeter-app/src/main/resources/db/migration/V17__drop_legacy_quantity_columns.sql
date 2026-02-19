-- V17: Remove legacy quantity columns
-- These were migration artifacts from converting DECIMAL to INTEGER.
-- No historical data to preserve.

ALTER TABLE stock_movements DROP COLUMN IF EXISTS quantity_old;
ALTER TABLE stock_movements DROP COLUMN IF EXISTS quantity_legacy;
