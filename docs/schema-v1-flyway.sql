-- ============================================================================
-- Novel Weaver — Flyway V1 初始化迁移 / 初期化マイグレーション / Initial Migration
-- ============================================================================
--
-- CN: 合并来源 A. .claude/schema.sql 的 7 张已验证生产表（6章数据）
--            B. novel-weaver/docs/design.md 的 6 张新表
--            C. 所有表加 project_id 实现多项目隔离
--
-- JP: 統合元 A. .claude/schema.sql の 7 つの検証済みプロダクションテーブル（6章分のデータ）
--            B. novel-weaver/docs/design.md の 6 つの新テーブル
--            C. 全テーブルに project_id を追加してマルチプロジェクト分離を実現
--
-- EN: Merged from A. 7 verified production tables from .claude/schema.sql (6 chapters)
--            B. 6 new tables from novel-weaver/docs/design.md
--            C. All tables get project_id for multi-project isolation
-- ============================================================================

-- ════════════════════════════════════════════════════════════════════════════
-- 扩展 / 拡張 / Extensions
-- ════════════════════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ════════════════════════════════════════════════════════════════════════════
-- 表 1: projects — 项目元信息（新規 / New）
-- CN: 项目元信息。每个项目（原创或同人小说）一条记录
-- JP: プロジェクトメタ情報。各プロジェクト（オリジナルまたは二次創作）に一つのレコード
-- EN: Project metadata. One record per project (original or fanfic)
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE projects (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    type        VARCHAR(10) NOT NULL CHECK (type IN ('original', 'fanfic')),
    status      VARCHAR(20) DEFAULT 'active',
    vault_path  TEXT,
    meta        JSONB DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 2: chapters — 章节主表（from schema.sql, +project_id）
-- CN: 章节正文 + 元数据。每章一条记录，含文件哈希用于增量判断
-- JP: チャプター本文＋メタデータ。各章一つのレコード、ファイルハッシュで増分判定
-- EN: Chapter body + metadata. One record per chapter, with file hash for incremental sync
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE chapters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    chapter_number  INT NOT NULL,
    title           TEXT NOT NULL,
    content         TEXT,
    phase           TEXT DEFAULT '',
    word_count      INT DEFAULT 0,
    summary         TEXT DEFAULT '',
    file_path       TEXT DEFAULT '',
    file_hash       TEXT,
    status          VARCHAR(20) DEFAULT 'draft',
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE (project_id, chapter_number)
);

CREATE INDEX idx_chapters_phase ON chapters (phase);
CREATE INDEX idx_chapters_number ON chapters (chapter_number);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 3: chapter_versions — 章节版本历史（新規 / New）
-- CN: 每次 chapter_sync 保存一个版本，用于回溯和差异对比
-- JP: chapter_sync のたびにバージョンを保存、遡及と差分比較に使用
-- EN: One version per chapter_sync save, for rollback and diff comparison
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE chapter_versions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id    UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    version       INT NOT NULL,
    content       TEXT,
    word_count    INT DEFAULT 0,
    file_hash     TEXT,
    created_at    TIMESTAMPTZ DEFAULT now(),
    UNIQUE (chapter_id, version)
);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 4: chapter_paragraphs — 场景段落 + 向量（from schema.sql, +project_id, vector列改pgvector原生）
-- CN: 分段后的段落级索引，含 embedding 向量，支持语义搜索
-- JP: 分割後の段落レベル索引、embedding ベクトル付き、意味検索に対応
-- EN: Paragraph-level index after segmentation, with embedding vectors for semantic search
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE chapter_paragraphs (
    id              SERIAL PRIMARY KEY,
    version_id      UUID NOT NULL REFERENCES chapter_versions(id) ON DELETE CASCADE,
    chapter_id      UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    seq             INT NOT NULL,
    scene           TEXT DEFAULT '',
    content         TEXT DEFAULT '',
    pov_character   TEXT DEFAULT '',
    scene_type      TEXT DEFAULT 'narrative',
    embedding       vector(1024),
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_paragraphs_chapter  ON chapter_paragraphs (chapter_id);
CREATE INDEX idx_paragraphs_project  ON chapter_paragraphs (project_id);
CREATE INDEX idx_paragraphs_scene    ON chapter_paragraphs (scene_type);
CREATE INDEX idx_paragraphs_pov      ON chapter_paragraphs (pov_character);
CREATE INDEX idx_paragraphs_embedding ON chapter_paragraphs
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 5: character_profiles — 人物画像（from schema.sql, +project_id, +voice_meta）
-- CN: 角色画像——含画像文件引用、声线种子、声线硬约束、向量 embedding
-- JP: キャラクター設定——設定ファイル参照、声線シード、声線ハード制約、ベクトル embedding
-- EN: Character profiles — includes profile file reference, voice seeds, voice constraints, vector embedding
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name                TEXT NOT NULL,
    name_jp             TEXT DEFAULT '',
    name_en             TEXT DEFAULT '',
    aliases             TEXT[] DEFAULT '{}',
    type                VARCHAR(20) DEFAULT 'major',
    bio                 TEXT DEFAULT '',
    profile_file        TEXT DEFAULT '',
    traits              JSONB DEFAULT '{}',
    voice               TEXT DEFAULT '',
    voice_seeds         TEXT[] DEFAULT '{}',
    voice_meta          JSONB DEFAULT '{}',
    status              JSONB DEFAULT '{}',
    basic_info          JSONB DEFAULT '{}',
    personality_traits  TEXT[] DEFAULT '{}',
    embedding           vector(1024),
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now(),
    UNIQUE (project_id, name)
);

CREATE INDEX idx_profiles_project ON character_profiles (project_id);
CREATE INDEX idx_profiles_embedding ON character_profiles
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

COMMENT ON COLUMN character_profiles.voice_seeds IS
  'CN: 手写2-3句标志性台词种子（如"少废话。"） / JP: 手書き2-3文の特徴的台詞シード / EN: 2-3 hand-written seed dialogues (e.g. "少废话。")';
COMMENT ON COLUMN character_profiles.voice_meta IS
  'CN: 声线硬约束JSON: {tone, max_sentence_length, forbidden_patterns, default_behavior, common_phrases, sentence_style} / JP: 声線ハード制約JSON / EN: Voice constraint JSON';

-- ════════════════════════════════════════════════════════════════════════════
-- 表 6: character_snapshots — 人物状态快照（from schema.sql, +project_id）
-- CN: 每章后记录角色位置、生理状态、心理状态，用于状态追踪
-- JP: 章ごとにキャラクターの位置、生理状態、心理状態を記録、状態追跡に使用
-- EN: Per-chapter record of character location, physical state, psychology — for state tracking
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_snapshots (
    id                  SERIAL PRIMARY KEY,
    character_id        UUID NOT NULL REFERENCES character_profiles(id) ON DELETE CASCADE,
    chapter_id          UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    character_name      TEXT NOT NULL,
    physical_location   TEXT DEFAULT '',
    physical_status     TEXT DEFAULT '',
    core_psychology     TEXT DEFAULT '',
    key_items           TEXT[] DEFAULT '{}',
    summary             TEXT DEFAULT '',
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now(),
    UNIQUE (chapter_id, character_id)
);

CREATE INDEX idx_snapshots_char ON character_snapshots (character_id);
CREATE INDEX idx_snapshots_chapter ON character_snapshots (chapter_id);
CREATE INDEX idx_snapshots_project ON character_snapshots (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 7: character_relationships — 人物关系（from schema.sql, +project_id）
-- CN: 角色间关系——类型、信任度、备注。双向记录
-- JP: キャラクター間の関係——タイプ、信頼度、備考。双方向記録
-- EN: Character relationships — type, trust level, notes. Bidirectional
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_relationships (
    id              SERIAL PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    from_char       TEXT NOT NULL,
    to_char         TEXT NOT NULL,
    relation_type   TEXT NOT NULL,
    trust_level     TEXT DEFAULT '',
    note            TEXT DEFAULT '',
    source_file     TEXT DEFAULT '',
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE (project_id, from_char, to_char, relation_type)
);

CREATE INDEX idx_relationships_from ON character_relationships (from_char);
CREATE INDEX idx_relationships_to ON character_relationships (to_char);
CREATE INDEX idx_relationships_project ON character_relationships (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 8: foreshadowing_index — 伏笔（from schema.sql, +project_id）
-- CN: 伏笔登记——类型（情感/身份/事件/道具）、埋设章节、状态
-- JP: 伏線登録——タイプ（感情/身分/イベント/アイテム）、設置章、状態
-- EN: Foreshadowing registry — type (emotional/identity/event/item), planted chapter, status
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE foreshadowing_index (
    id                  SERIAL PRIMARY KEY,
    project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    code                TEXT NOT NULL,
    description         TEXT DEFAULT '',
    f_type              TEXT DEFAULT '',
    planted_chapter     INT DEFAULT 1,
    payoff_chapter      INT DEFAULT NULL,
    status              TEXT DEFAULT 'active',
    related_characters  TEXT[] DEFAULT '{}',
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now(),
    UNIQUE (project_id, code)
);

CREATE INDEX idx_foreshadowing_project ON foreshadowing_index (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 9: character_voiceprints — 声纹样本（新規 / New）
-- CN: 角色声纹样本——手动种子 / 正文提取 / 正典来源，含向量用于相似场景检索
-- JP: キャラクター声紋サンプル——手動シード / 本文抽出 / 正典ソース、類似シーン検索用ベクトル付き
-- EN: Character voiceprint samples — seed / extracted / canon, with vectors for similar-scene retrieval
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_voiceprints (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id  UUID NOT NULL REFERENCES character_profiles(id) ON DELETE CASCADE,
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    chapter_id    UUID REFERENCES chapters(id),
    dialogue      TEXT NOT NULL,
    source        VARCHAR(20) DEFAULT 'seed',     -- seed / extracted / canon
    embedding     vector(1024),
    meta          JSONB DEFAULT '{}',
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_voiceprints_char ON character_voiceprints (character_id);
CREATE INDEX idx_voiceprints_project ON character_voiceprints (project_id);
CREATE INDEX idx_voiceprints_embedding ON character_voiceprints
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 10: canon_sources — 正典来源（新規 / New）
-- CN: 正典资料出处——游戏、书籍等来源，标记是否已人工审核
-- JP: 正典資料の出典——ゲーム、書籍などのソース、人為チェック済みかどうかを記録
-- EN: Canon source references — game, book, etc., with verification status
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_sources (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    url           TEXT DEFAULT '',
    verified      BOOLEAN DEFAULT false,
    created_at    TIMESTAMPTZ DEFAULT now(),
    UNIQUE (project_id, name)
);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 11: canon_characters — 正典人物（新規 / New）
-- CN: 正典人物——同人小说的红线和事实基础。含向量用于相似度匹配
-- JP: 正典キャラクター——二次創作の制約と事実基盤。類似度マッチング用ベクトル付き
-- EN: Canon characters — red-line constraints and factual basis for fanfic. With embedding vectors
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_characters (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_id     UUID REFERENCES canon_sources(id),
    name          TEXT NOT NULL,
    aliases       TEXT[] DEFAULT '{}',
    bio           TEXT DEFAULT '',
    traits        JSONB DEFAULT '{}',
    embedding     vector(1024),
    verified      BOOLEAN DEFAULT false,
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_canon_char_project ON canon_characters (project_id);
CREATE INDEX idx_canon_char_embedding ON canon_characters
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 12: canon_events — 正典事件（新規 / New）
-- CN: 正典事件——按约束等级标记（🔴不可改 / 🟡可微调 / 🟢可原创）
-- JP: 正典イベント——制約レベルで標記（🔴変更不可 / 🟡微調整可 / 🟢原創可）
-- EN: Canon events — tagged by constraint level (🔴 immutable / 🟡 adjustable / 🟢 original-allowed)
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_id     UUID REFERENCES canon_sources(id),
    name          TEXT NOT NULL,
    timeline_pos  TEXT DEFAULT '',
    description   TEXT DEFAULT '',
    participants  TEXT[] DEFAULT '{}',
    canon_level   VARCHAR(5) DEFAULT '🟢',        -- 🔴 不可改 / 🟡 可微调 / 🟢 可原创
    embedding     vector(1024),
    verified      BOOLEAN DEFAULT false,
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_canon_event_project ON canon_events (project_id);
CREATE INDEX idx_canon_event_embedding ON canon_events
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 13: canon_relationships — 正典人物关系（新規 / New）
-- CN: 正典中的人物关系——同人写作的红线参考
-- JP: 正典におけるキャラクター関係——二次創作の制約参照
-- EN: Canon character relationships — red-line reference for fanfic writing
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_relationships (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_id     UUID REFERENCES canon_sources(id),
    from_char     TEXT NOT NULL,
    to_char       TEXT NOT NULL,
    rel_type      TEXT NOT NULL,
    description   TEXT DEFAULT '',
    verified      BOOLEAN DEFAULT false,
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_canon_rel_project ON canon_relationships (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 14: timelines — 时间线（新規 / New）
-- CN: 时间线定义——主线、回忆线、分支、时间闭环
-- JP: タイムライン定義——メイン、回想、分岐、時間ループ
-- EN: Timeline definitions — main, flashback, branch, time loop
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE timelines (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name          TEXT NOT NULL DEFAULT 'main',
    type          VARCHAR(20) DEFAULT 'main',      -- main / flashback / branch / loop
    description   TEXT DEFAULT '',
    created_at    TIMESTAMPTZ DEFAULT now()
);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 15: timeline_events — 时间线事件（新規 / New）
-- CN: 时间线中的事件——按实际时间和叙述顺序双索引，支持复杂时间线建模
-- JP: タイムライン内のイベント——実時間と叙述順の二重索引、複雑なタイムラインをモデリング
-- EN: Timeline events — dual-indexed by absolute and narrative order, for complex timeline modeling
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE timeline_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    timeline_id     UUID NOT NULL REFERENCES timelines(id) ON DELETE CASCADE,
    chapter_id      UUID REFERENCES chapters(id),
    name            TEXT NOT NULL,
    absolute_order  INT,
    narrative_order INT,
    description     TEXT DEFAULT '',
    is_canon        BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_timeline_event_project ON timeline_events (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 16: deduction_logs — 推演日志（新規 / New）
-- CN: 推演记录——类型、模型、token数、成本，用于质量追踪和成本控制
-- JP: 推論記録——タイプ、モデル、トークン数、コスト、品質追跡とコスト管理に使用
-- EN: Deduction logs — type, model, token counts, cost, for quality tracking and cost control
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE deduction_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    chapter_id    UUID REFERENCES chapters(id),
    type          VARCHAR(30) NOT NULL,            -- behavior / outline / verify
    model         TEXT DEFAULT '',
    input_context TEXT DEFAULT '',
    output_result TEXT DEFAULT '',
    tokens_in     INT DEFAULT 0,
    tokens_out    INT DEFAULT 0,
    cost          DECIMAL(10,6) DEFAULT 0,
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_deduction_project ON deduction_logs (project_id);
CREATE INDEX idx_deduction_chapter ON deduction_logs (chapter_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 22: items
-- ════════════════════════════════════════════════════════════════════════════

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

-- ════════════════════════════════════════════════════════════════════════════
-- 验证查询 / 確認クエリ / Verification Query
-- ════════════════════════════════════════════════════════════════════════════

-- CN: 列出所有表以确认迁移完成
-- JP: 全テーブルを一覧表示してマイグレーション完了を確認
-- EN: List all tables to verify migration completion
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' ORDER BY table_name;
