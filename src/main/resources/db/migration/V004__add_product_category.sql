-- Nullable column: existing code that inserts products without a category
-- will have NULL stored, which is acceptable. This makes the migration
-- backward-compatible with any code that does not yet know about category.
ALTER TABLE product ADD COLUMN category VARCHAR(50);
