-- Backfill existing rows with 'GENERAL' so the NOT NULL constraint is satisfiable,
-- then drop the DEFAULT so new inserts must supply a category explicitly.
-- NOTE: this migration is intentionally backward-incompatible with existing code that
-- inserts products without a category (see V004 amendment for the compatible version).
ALTER TABLE product ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE product ALTER COLUMN category DROP DEFAULT;
