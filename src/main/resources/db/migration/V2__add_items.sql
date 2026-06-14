-- ============================================================================
-- Novel Weaver — Flyway V2 新增物品表 / アイテムテーブル追加 / Add Items Table
-- ============================================================================
--
-- CN: 新增 items 表——物品档案（外观、来源、归属历史、当前状态、语义搜索）
-- JP: items テーブルを追加——アイテムアーカイブ（外観、由来、所有履歴、現在状態、意味検索）
-- EN: Add items table — item archive (appearance, origin, ownership history, current status, semantic search)
-- ============================================================================

CREATE TABLE items
(
    id               UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id       UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name             TEXT NOT NULL,
    item_type        TEXT             DEFAULT '',
    description      TEXT             DEFAULT '',
    origin           TEXT             DEFAULT '',
    significance     TEXT             DEFAULT '',
    properties       JSONB            DEFAULT '{}'::jsonb,
    current_holder   TEXT             DEFAULT '',
    current_location TEXT             DEFAULT '',
    current_status   TEXT             DEFAULT '正常',
    first_chapter    INT              DEFAULT 1,
    owner_history    JSONB            DEFAULT '[]'::jsonb,
    embedding        vector(1024),
    created_at       TIMESTAMPTZ      DEFAULT now(),
    updated_at       TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, name)
);

COMMENT
ON TABLE items IS '物品档案——外观、来源、归属历史、当前状态
JP アイテムアーカイブ——外観、由来、所有履歴、現在状態
EN Item archive — appearance, origin, ownership history, current status';
COMMENT
ON COLUMN items.item_type IS '武器/信物/神器/日常物品/其他
JP 武器/形見/神器/日常品/その他
EN weapon/token/artifact/everyday/other';
COMMENT
ON COLUMN items.owner_history IS '归属变更历史 JSON 数组 [{"chapter":1,"from":"锻造者","to":"主角","event":"赠予"}]';

CREATE INDEX idx_items_project ON items (project_id);
CREATE INDEX idx_items_embedding ON items USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- universe_id for future cross-universe items
ALTER TABLE items
    ADD COLUMN IF NOT EXISTS universe_id UUID REFERENCES universes(id);
CREATE INDEX IF NOT EXISTS idx_items_universe ON items (universe_id);
