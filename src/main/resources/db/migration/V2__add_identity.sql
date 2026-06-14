-- V2: Add identity JSONB column and remove name-based unique constraints
-- Allows same-name entities distinguished by an identity JSON field.

-- ═══ character_profiles ═══
ALTER TABLE character_profiles DROP CONSTRAINT IF EXISTS character_profiles_project_id_name_key;
ALTER TABLE character_profiles
    ADD COLUMN IF NOT EXISTS identity JSONB DEFAULT '{}';
CREATE INDEX IF NOT EXISTS idx_profiles_identity ON character_profiles USING GIN (identity);

-- ═══ items ═══
ALTER TABLE items DROP CONSTRAINT IF EXISTS items_project_id_name_key;
ALTER TABLE items
    ADD COLUMN IF NOT EXISTS identity JSONB DEFAULT '{}';
CREATE INDEX IF NOT EXISTS idx_items_identity ON items USING GIN (identity);

-- ═══ locations ═══
ALTER TABLE locations DROP CONSTRAINT IF EXISTS locations_project_id_name_key;
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS identity JSONB DEFAULT '{}';
CREATE INDEX IF NOT EXISTS idx_locations_identity ON locations USING GIN (identity);
