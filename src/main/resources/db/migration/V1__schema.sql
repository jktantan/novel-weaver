-- ============================================================================
-- Novel Weaver — Flyway V1 初始化迁移
-- 19 张表 + pgvector 扩展
-- ============================================================================

CREATE
EXTENSION IF NOT EXISTS vector;

-- ════════════════════════════════════════════════════════════════════════════
-- 表 1: projects
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE projects
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    name       TEXT        NOT NULL,
    type       VARCHAR(10) NOT NULL CHECK (type IN ('original', 'fanfic')),
    status     VARCHAR(20)      DEFAULT 'active',
    vault_path TEXT,
    meta       JSONB            DEFAULT '{}',
    created_at TIMESTAMPTZ      DEFAULT now(),
    updated_at TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE projects IS '项目元信息——每个项目包含一部小说的全部数据
JP プロジェクトメタ情報——各プロジェクトは1つの小説の全データを含む
EN Project metadata — each project contains all data for one novel';
COMMENT
ON COLUMN projects.type IS 'original(原创) / fanfic(同人)
JP original(オリジナル) / fanfic(二次創作)
EN original / fanfic';
COMMENT
ON COLUMN projects.status IS 'active / archived
JP active(アクティブ) / archived(アーカイブ)
EN active / archived';
COMMENT
ON COLUMN projects.meta IS '额外元数据 JSON（主角名、视角、写作约束等）
JP 追加メタデータJSON（主人公名、視点、執筆制約など）
EN Extra metadata JSON (protagonist, POV, writing constraints, etc.)';

-- ════════════════════════════════════════════════════════════════════════════
-- 表 2: chapters
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE chapters
(
    id             UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id     UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    chapter_number INT  NOT NULL,
    title          TEXT NOT NULL,
    content        TEXT,
    phase          TEXT             DEFAULT '',
    word_count     INT              DEFAULT 0,
    summary        TEXT             DEFAULT '',
    file_path      TEXT             DEFAULT '',
    file_hash      TEXT,
    status         VARCHAR(20)      DEFAULT 'draft',
    created_at     TIMESTAMPTZ      DEFAULT now(),
    updated_at     TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, chapter_number)
);

COMMENT
ON TABLE chapters IS '章节正文+元数据——故事主体的最小单元
JP 章の本文+メタデータ——物語の最小単位
EN Chapter content + metadata — the smallest unit of story';
COMMENT
ON COLUMN chapters.phase IS '所属阶段：降临期/旁观期/石化期/回归期/之后
JP フェーズ：降临期/旁观期/石化期/回归期/之后
EN Phase: arrival/observation/petrification/return/after';
COMMENT
ON COLUMN chapters.content IS '完整正文
JP 完全な本文
EN Full text content';
COMMENT
ON COLUMN chapters.status IS 'draft(草稿) / review(审查中) / final(定稿)
JP draft(下書き) / review(レビュー中) / final(確定稿)
EN draft / review / final';

CREATE INDEX idx_chapters_phase ON chapters (phase);
CREATE INDEX idx_chapters_number ON chapters (chapter_number);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 3: chapter_versions
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE chapter_versions
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    chapter_id UUID NOT NULL REFERENCES chapters (id) ON DELETE CASCADE,
    version    INT  NOT NULL,
    content    TEXT,
    word_count INT              DEFAULT 0,
    file_hash  TEXT,
    created_at TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (chapter_id, version)
);

COMMENT
ON TABLE chapter_versions IS '章节版本历史——每次 chapter_sync 自动生成一个新版本
JP 章のバージョン履歴——chapter_syncのたびに新バージョンが自動生成される
EN Chapter version history — a new version is auto-generated on each chapter_sync';

-- ════════════════════════════════════════════════════════════════════════════
-- 表 4: chapter_paragraphs
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE chapter_paragraphs
(
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    version_id    UUID NOT NULL REFERENCES chapter_versions (id) ON DELETE CASCADE,
    chapter_id    UUID NOT NULL REFERENCES chapters (id) ON DELETE CASCADE,
    project_id    UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    seq           INT  NOT NULL,
    scene         TEXT             DEFAULT '',
    content       TEXT             DEFAULT '',
    pov_character TEXT             DEFAULT '',
    scene_type    TEXT             DEFAULT 'narrative',
    embedding     vector(1024),
    created_at    TIMESTAMPTZ      DEFAULT now(),
    updated_at    TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE chapter_paragraphs IS '段落级语义搜索单元（约500-800字/段）
JP 段落レベルの意味検索ユニット（約500-800字/段落）
EN Paragraph-level semantic search unit (~500-800 chars/paragraph)';
COMMENT
ON COLUMN chapter_paragraphs.seq IS '段落顺序，章节内递增
JP 段落順序、章内で増加
EN Paragraph sequence number, incremented within chapter';
COMMENT
ON COLUMN chapter_paragraphs.scene IS '所属场景标题
JP 所属シーンタイトル
EN Scene title';
COMMENT
ON COLUMN chapter_paragraphs.scene_type IS 'narrative(叙述)/action(动作)/dialogue(对话)/interior(内心)
JP narrative(叙述)/action(動作)/dialogue(会話)/interior(内心)
EN narrative/action/dialogue/interior';
COMMENT
ON COLUMN chapter_paragraphs.pov_character IS '视角人物
JP 視点人物
EN POV character';
COMMENT
ON COLUMN chapter_paragraphs.embedding IS 'bge-m3 向量(1024维)，语义检索用
JP bge-m3 ベクトル(1024次元)、意味検索用
EN bge-m3 vector (1024-dim), for semantic search';

CREATE INDEX idx_paragraphs_chapter ON chapter_paragraphs (chapter_id);
CREATE INDEX idx_paragraphs_project ON chapter_paragraphs (project_id);
CREATE INDEX idx_paragraphs_scene ON chapter_paragraphs (scene_type);
CREATE INDEX idx_paragraphs_pov ON chapter_paragraphs (pov_character);
CREATE INDEX idx_paragraphs_embedding ON chapter_paragraphs
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 5: character_profiles
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_profiles
(
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id         UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name               TEXT NOT NULL,
    name_jp            TEXT             DEFAULT '',
    name_en            TEXT             DEFAULT '',
    aliases            TEXT[] DEFAULT '{}',
    type               VARCHAR(20)      DEFAULT 'major',
    bio                TEXT             DEFAULT '',
    profile_file       TEXT             DEFAULT '',
    traits             JSONB            DEFAULT '{}',
    voice              TEXT             DEFAULT '',
    voice_seeds        TEXT[] DEFAULT '{}',
    voice_meta         JSONB            DEFAULT '{}',
    status             JSONB            DEFAULT '{}',
    basic_info         JSONB            DEFAULT '{}',
    personality_traits TEXT[] DEFAULT '{}',
    embedding          vector(1024),
    created_at         TIMESTAMPTZ      DEFAULT now(),
    updated_at         TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, name)
);

COMMENT
ON TABLE character_profiles IS '人物画像——包含性格特征、声线约束、当前状态
JP キャラクタープロフィール——性格特性、声色制約、現在の状態を含む
EN Character profile — includes personality traits, voice constraints, current status';
COMMENT
ON COLUMN character_profiles.type IS 'major(主角) / minor(配角) / extra(龙套)
JP major(主人公) / minor(脇役) / extra(端役)
EN major / minor / extra';
COMMENT
ON COLUMN character_profiles.traits IS '性格特征数组 JSON
JP 性格特性配列 JSON
EN Personality traits JSON array';
COMMENT
ON COLUMN character_profiles.voice IS '声线自然语言描述
JP 声色の自然言語による説明
EN Voice natural language description';
COMMENT
ON COLUMN character_profiles.voice_seeds IS '标志性台词种子（如"少废话。"）';
COMMENT
ON COLUMN character_profiles.voice_meta IS '声线硬约束 JSON: {tone, max_sentence_length, forbidden_patterns}
JP 声色のハード制約 JSON: {tone, max_sentence_length, forbidden_patterns}
EN Voice hard constraints JSON: {tone, max_sentence_length, forbidden_patterns}';

CREATE INDEX idx_profiles_project ON character_profiles (project_id);
CREATE INDEX idx_profiles_embedding ON character_profiles
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 6: character_snapshots
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_snapshots
(
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    character_id      UUID NOT NULL REFERENCES character_profiles (id) ON DELETE CASCADE,
    chapter_id        UUID NOT NULL REFERENCES chapters (id) ON DELETE CASCADE,
    project_id        UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    character_name    TEXT NOT NULL,
    physical_location TEXT             DEFAULT '',
    physical_status   TEXT             DEFAULT '',
    core_psychology   TEXT             DEFAULT '',
    key_items         TEXT[] DEFAULT '{}',
    summary           TEXT             DEFAULT '',
    created_at        TIMESTAMPTZ      DEFAULT now(),
    updated_at        TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (chapter_id, character_id)
);

COMMENT
ON TABLE character_snapshots IS '角色状态快照——每章结束后记录一次
JP キャラクター状態スナップショット——各章終了後に記録
EN Character state snapshot — recorded after each chapter';

CREATE INDEX idx_snapshots_char ON character_snapshots (character_id);
CREATE INDEX idx_snapshots_chapter ON character_snapshots (chapter_id);
CREATE INDEX idx_snapshots_project ON character_snapshots (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 7: character_relationships
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_relationships
(
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id    UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    from_char     TEXT NOT NULL,
    to_char       TEXT NOT NULL,
    relation_type TEXT NOT NULL,
    trust_level   TEXT             DEFAULT '',
    note          TEXT             DEFAULT '',
    source_file   TEXT             DEFAULT '',
    created_at    TIMESTAMPTZ      DEFAULT now(),
    updated_at    TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, from_char, to_char, relation_type)
);

COMMENT
ON TABLE character_relationships IS '人物关系表（PSQL 端，与 Neo4j 互补）
JP 人物関係テーブル（PSQL側、Neo4jと補完関係）
EN Character relationships table (PSQL side, complementary with Neo4j)';

CREATE INDEX idx_relationships_from ON character_relationships (from_char);
CREATE INDEX idx_relationships_to ON character_relationships (to_char);
CREATE INDEX idx_relationships_project ON character_relationships (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 8: foreshadowing_index
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE foreshadowing_index
(
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id         UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    code               TEXT NOT NULL,
    description        TEXT             DEFAULT '',
    f_type             TEXT             DEFAULT '',
    planted_chapter    INT              DEFAULT 1,
    payoff_chapter     INT              DEFAULT NULL,
    status             TEXT             DEFAULT 'active',
    related_characters TEXT[] DEFAULT '{}',
    created_at         TIMESTAMPTZ      DEFAULT now(),
    updated_at         TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, code)
);

COMMENT
ON TABLE foreshadowing_index IS '伏笔管理系统——登记/追踪/回收
JP 伏線管理システム——登録/追跡/回収
EN Foreshadowing management — register/track/close';
COMMENT
ON COLUMN foreshadowing_index.f_type IS '🔮情感(感情) / 🎭身份(身分) / 🎯事件(事件) / 💡道具(道具)
JP 🔮感情 / 🎭身分 / 🎯事件 / 💡道具
EN 🔮emotional / 🎭identity / 🎯event / 💡item';
COMMENT
ON COLUMN foreshadowing_index.status IS 'active(活跃)/triggered(已触发)/closed(已关闭)
JP active(アクティブ)/triggered(発動済)/closed(クローズ)
EN active / triggered / closed';

CREATE INDEX idx_foreshadowing_project ON foreshadowing_index (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 9: character_voiceprints
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE character_voiceprints
(
    id           UUID PRIMARY KEY DEFAULT uuidv7(),
    character_id UUID NOT NULL REFERENCES character_profiles (id) ON DELETE CASCADE,
    project_id   UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    chapter_id   UUID REFERENCES chapters (id),
    dialogue     TEXT NOT NULL,
    source       VARCHAR(20)      DEFAULT 'seed',
    embedding    vector(1024),
    meta         JSONB            DEFAULT '{}',
    created_at   TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE character_voiceprints IS '声纹样本库——角色的标志性台词及其向量表示
JP 声紋サンプル庫——キャラクターの特徴的台詞とそのベクトル表現
EN Voiceprint library — character signature lines and their vector representations';
COMMENT
ON COLUMN character_voiceprints.source IS 'seed(种子)/extracted(提取)/canon(正典)
JP seed(種)/extracted(抽出)/canon(正典)
EN seed / extracted / canon';

CREATE INDEX idx_voiceprints_char ON character_voiceprints (character_id);
CREATE INDEX idx_voiceprints_project ON character_voiceprints (project_id);
CREATE INDEX idx_voiceprints_embedding ON character_voiceprints
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 10: canon_sources
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_sources
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name       TEXT NOT NULL,
    url        TEXT             DEFAULT '',
    verified   BOOLEAN          DEFAULT false,
    created_at TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, name)
);

COMMENT
ON TABLE canon_sources IS '正典资料来源记录——仅同人项目使用
JP 正典資料ソース記録——二次創作プロジェクトのみ使用
EN Canon source record — used only for fanfic projects';

-- ════════════════════════════════════════════════════════════════════════════
-- 表 11: canon_characters
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_characters
(
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    source_id  UUID REFERENCES canon_sources (id),
    name       TEXT NOT NULL,
    aliases    TEXT[] DEFAULT '{}',
    bio        TEXT             DEFAULT '',
    traits     JSONB            DEFAULT '{}',
    embedding  vector(1024),
    verified   BOOLEAN          DEFAULT false,
    created_at TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE canon_characters IS '正典人物——从原作提取的不可变更的人物事实
JP 正典キャラクター——原作から抽出した不変の人物情報
EN Canon characters — immutable character facts extracted from source';
COMMENT
ON COLUMN canon_characters.verified IS '是否已人工审核确认
JP 人手による確認済みフラグ
EN Whether verified by human review';

CREATE INDEX idx_canon_char_project ON canon_characters (project_id);
CREATE INDEX idx_canon_char_embedding ON canon_characters
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 12: canon_events
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_events
(
    id           UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id   UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    source_id    UUID REFERENCES canon_sources (id),
    name         TEXT NOT NULL,
    timeline_pos TEXT             DEFAULT '',
    description  TEXT             DEFAULT '',
    participants TEXT[] DEFAULT '{}',
    date_label   TEXT             DEFAULT '',
    canon_level  VARCHAR(5)       DEFAULT '🟢',
    embedding    vector(1024),
    verified     BOOLEAN          DEFAULT false,
    created_at   TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE canon_events IS '正典事件——从原作提取的不可变更的事件事实
JP 正典イベント——原作から抽出した不変の出来事
EN Canon events — immutable event facts extracted from source';
COMMENT
ON COLUMN canon_events.timeline_pos IS '在原作时间线中的位置描述
JP 原作タイムライン上の位置
EN Position in the original timeline';
COMMENT
ON COLUMN canon_events.date_label IS '时间标签（如"2003年""3465年"）';
COMMENT
ON COLUMN canon_events.canon_level IS '🔴不可改(変更不可) / 🟡可微调(微調整可) / 🟢可自由创作(自由創作可)
JP 🔴変更不可 / 🟡微調整可 / 🟢自由創作可
EN 🔴immutable / 🟡adjustable / 🟢free to create';

CREATE INDEX idx_canon_event_project ON canon_events (project_id);
CREATE INDEX idx_canon_event_embedding ON canon_events
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 13: canon_relationships
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_relationships
(
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id  UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    source_id   UUID REFERENCES canon_sources (id),
    from_char   TEXT NOT NULL,
    to_char     TEXT NOT NULL,
    rel_type    TEXT NOT NULL,
    description TEXT             DEFAULT '',
    verified    BOOLEAN          DEFAULT false,
    created_at  TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE canon_relationships IS '正典人物关系——不可变更的原作关系
JP 正典人物関係——変更不可の原作関係
EN Canon relationships — immutable original relationships';

CREATE INDEX idx_canon_rel_project ON canon_relationships (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 14: timelines
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE timelines
(
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id  UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name        TEXT NOT NULL    DEFAULT 'main',
    type        VARCHAR(20)      DEFAULT 'main',
    description TEXT             DEFAULT '',
    created_at  TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE timelines IS '时间线定义——支持多条并列时间线
JP タイムライン定義——複数並行タイムラインをサポート
EN Timeline definition — supports multiple parallel timelines';
COMMENT
ON COLUMN timelines.type IS 'main(主线)/flashback(回忆)/branch(分支)/loop(闭环)/alternative(平行)
JP main(本筋)/flashback(回想)/branch(分岐)/loop(ループ)/alternative(並行)
EN main/flashback/branch/loop/alternative';

-- ════════════════════════════════════════════════════════════════════════════
-- 表 15: timeline_events
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE timeline_events
(
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id      UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    timeline_id     UUID NOT NULL REFERENCES timelines (id) ON DELETE CASCADE,
    chapter_id      UUID REFERENCES chapters (id),
    name            TEXT NOT NULL,
    absolute_order  INT,
    narrative_order INT,
    description     TEXT             DEFAULT '',
    date_label      TEXT             DEFAULT '',
    is_canon        BOOLEAN          DEFAULT false,
    created_at      TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE timeline_events IS '时间线事件——按绝对顺序和叙述顺序排列
JP タイムラインイベント——絶対順序と叙述順序で整列
EN Timeline events — ordered by absolute and narrative sequence';
COMMENT
ON COLUMN timeline_events.absolute_order IS '实际发生顺序
JP 実際の発生順序
EN Absolute chronological order';
COMMENT
ON COLUMN timeline_events.narrative_order IS '叙述呈现顺序（与absolute不同时为插叙/倒叙）
JP 叙述順序（絶対順序と異なる場合は挿入/逆転）
EN Narrative order (differs from absolute when using non-linear storytelling)';
COMMENT
ON COLUMN timeline_events.date_label IS '时间标签（"2003年""星历元年"等自由文本）
JP 時間ラベル（「2003年」「星歴元年」などの自由テキスト）
EN Date label (free text like "2003", "Star Era 1")';
COMMENT
ON COLUMN timeline_events.is_canon IS '属于正典事件还是同人原创事件
JP 正典イベントか二次創作オリジナルイベントか
EN Whether this belongs to canon or original fanfic events';

CREATE INDEX idx_timeline_event_project ON timeline_events (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 16: deduction_logs
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE deduction_logs
(
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id    UUID        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    chapter_id    UUID REFERENCES chapters (id),
    type          VARCHAR(30) NOT NULL,
    model         TEXT             DEFAULT '',
    input_context TEXT             DEFAULT '',
    output_result TEXT             DEFAULT '',
    tokens_in     INT              DEFAULT 0,
    tokens_out    INT              DEFAULT 0,
    cost          DECIMAL(10, 6)   DEFAULT 0,
    created_at    TIMESTAMPTZ      DEFAULT now()
);

COMMENT
ON TABLE deduction_logs IS '推演日志——记录所有推演操作的历史
JP 推論ログ——すべての推論操作の履歴を記録
EN Deduction log — records all deduction operation history';
COMMENT
ON COLUMN deduction_logs.type IS 'behavior(角色推演)/outline(大纲推演)/verify(验证)
JP behavior(行動推論)/outline(アウトライン推論)/verify(検証)
EN behavior/outline/verify';

CREATE INDEX idx_deduction_project ON deduction_logs (project_id);
CREATE INDEX idx_deduction_chapter ON deduction_logs (chapter_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 17: locations
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE locations
(
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id         UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name               TEXT NOT NULL,
    location_type      TEXT             DEFAULT '',
    region             TEXT             DEFAULT '',
    first_chapter      INT              DEFAULT 1,
    canon_description  TEXT             DEFAULT '',
    actual_appearance  TEXT             DEFAULT '',
    sensory_detail     TEXT             DEFAULT '',
    narrative_function TEXT             DEFAULT '',
    change_log         JSONB            DEFAULT '[]'::jsonb,
    current_status     TEXT             DEFAULT '正常',
    created_at         TIMESTAMPTZ      DEFAULT now(),
    updated_at         TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, name)
);

COMMENT
ON TABLE locations IS '地点档案——初始外观、变更历史、当前状态的完整追踪
JP 場所アーカイブ——初期外観、変更履歴、現在状態の完全追跡
EN Location archive — full tracking of initial appearance, change history, current status';
COMMENT
ON COLUMN locations.location_type IS '自然场景/建筑/聚落/室内
JP 自然景観/建築物/集落/室内
EN natural scene/building/settlement/indoor';
COMMENT
ON COLUMN locations.canon_description IS '正典/原始设定中的描述（同人可查wiki）
JP 正典/原典設定の説明（二次創作はwiki参照可）
EN Canon/original description (fanfics can reference wiki)';
COMMENT
ON COLUMN locations.actual_appearance IS '叙事视角下的真实面貌
JP 叙事視点から見た実際の外観
EN Actual appearance from narrative perspective';
COMMENT
ON COLUMN locations.sensory_detail IS '视觉/听觉/嗅觉/触觉细节
JP 視覚/聴覚/嗅覚/触覚の詳細
EN Visual/auditory/olfactory/tactile details';
COMMENT
ON COLUMN locations.change_log IS '变更记录JSON数组:[{chapter,trigger_event,change,new_status}]
JP 変更記録JSON配列:[{chapter,trigger_event,change,new_status}]
EN Change log JSON array: [{chapter,trigger_event,change,new_status}]';
COMMENT
ON COLUMN locations.current_status IS '当前状态（由change_log最后一条冗余）
JP 現在状態（change_logの最終エントリから冗長保持）
EN Current status (denormalized from last change_log entry)';

CREATE INDEX idx_locations_project ON locations (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 18: canon_event_status
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE canon_event_status
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id          UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    canon_event_id      UUID NOT NULL REFERENCES canon_events (id) ON DELETE CASCADE,
    status              TEXT NOT NULL    DEFAULT 'pending'
        CHECK (status IN ('pending', 'triggered', 'modified', 'skipped')),
    actual_description  TEXT             DEFAULT '',
    occurred_in_chapter INT,
    divergence_reason   TEXT             DEFAULT '',
    created_at          TIMESTAMPTZ      DEFAULT now(),
    updated_at          TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, canon_event_id)
);

COMMENT
ON TABLE canon_event_status IS '正典事件在同人小说中的实际走向追踪
JP 正典イベントの二次創作における実際の展開追跡
EN Tracks how canon events actually play out in fanfic';
COMMENT
ON COLUMN canon_event_status.status IS 'pending(未触发)/triggered(按正典发生)/modified(被改变)/skipped(跳过)
JP pending(未発動)/triggered(正典通り発生)/modified(変更)/skipped(スキップ)
EN pending/triggered/modified/skipped';
COMMENT
ON COLUMN canon_event_status.divergence_reason IS '与正典不同的原因（如"古月干预"）
JP 正典と異なる理由（例：「古月の介入」）
EN Reason for divergence (e.g. "character intervention")';

CREATE INDEX idx_canon_evt_status_project ON canon_event_status (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 19: timeline_links
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE timeline_links
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id          UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    from_timeline_id    UUID NOT NULL REFERENCES timelines (id) ON DELETE CASCADE,
    to_timeline_id      UUID NOT NULL REFERENCES timelines (id) ON DELETE CASCADE,
    link_type           TEXT NOT NULL    DEFAULT 'time_jump_from'
        CHECK (link_type IN ('flashback_of', 'alternative_to', 'time_jump_from')),
    from_absolute_order INT,
    to_absolute_order   INT,
    description         TEXT             DEFAULT '',
    created_at          TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, from_timeline_id, to_timeline_id)
);

COMMENT
ON TABLE timeline_links IS '时间线之间的关联——跳转、回忆、分支
JP タイムライン間の関連——跳躍、回想、分岐
EN Timeline links — jumps, flashbacks, branches';
COMMENT
ON COLUMN timeline_links.link_type IS 'flashback_of(回忆)/alternative_to(平行分支)/time_jump_from(时间跳转)
JP flashback_of(回想)/alternative_to(並行分岐)/time_jump_from(時間跳躍)
EN flashback_of/flashback/alternative_to/parallel/time_jump_from/time travel';
COMMENT
ON COLUMN timeline_links.from_absolute_order IS '源时间线的事件顺序号，从哪个位置离开
JP 元タイムラインのイベント順序番号、どの位置から離脱したか
EN Source timeline event order — where the jump leaves from';
COMMENT
ON COLUMN timeline_links.to_absolute_order IS '目标时间线的事件顺序号，到哪个位置接入
JP 先タイムラインのイベント順序番号、どの位置に接続するか
EN Target timeline event order — where the jump arrives at';

CREATE INDEX idx_timeline_links_project ON timeline_links (project_id);
CREATE INDEX idx_timeline_links_from ON timeline_links (from_timeline_id);
CREATE INDEX idx_timeline_links_to ON timeline_links (to_timeline_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 20: universes
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE universes
(
    id          UUID PRIMARY KEY     DEFAULT uuidv7(),
    project_id  UUID        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name        TEXT        NOT NULL,
    type        VARCHAR(20) NOT NULL DEFAULT 'original'
        CHECK (type IN ('original', 'fanfic', 'crossover')),
    description TEXT                 DEFAULT '',
    created_at  TIMESTAMPTZ          DEFAULT now(),
    updated_at  TIMESTAMPTZ          DEFAULT now(),
    UNIQUE (project_id, name)
);

COMMENT
ON TABLE universes IS '宇宙——作品世界观容器，一个项目可有多个宇宙
JP 宇宙——作品世界観のコンテナ、1プロジェクトで複数の宇宙を持てる
EN Universe — world-building container, a project can have multiple universes';
COMMENT
ON COLUMN universes.type IS 'original(原创)/fanfic(同人)/crossover(融合)
JP original(オリジナル)/fanfic(二次創作)/crossover(クロスオーバー)
EN original/fanfic/crossover';

CREATE INDEX idx_universes_project ON universes (project_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 表 21: universe_relations
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE universe_relations
(
    id               UUID PRIMARY KEY DEFAULT uuidv7(),
    project_id       UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    from_universe_id UUID NOT NULL REFERENCES universes (id) ON DELETE CASCADE,
    to_universe_id   UUID NOT NULL REFERENCES universes (id) ON DELETE CASCADE,
    relation_type    TEXT NOT NULL    DEFAULT 'parallel_to'
        CHECK (relation_type IN ('parallel_to', 'derived_from', 'crosses_over_with')),
    description      TEXT             DEFAULT '',
    created_at       TIMESTAMPTZ      DEFAULT now(),
    UNIQUE (project_id, from_universe_id, to_universe_id)
);

COMMENT
ON TABLE universe_relations IS '宇宙之间的关系——平行、衍生、跨界
JP 宇宙間の関係——並行、派生、クロス
EN Universe relations — parallel, derived, crossover';
COMMENT
ON COLUMN universe_relations.relation_type IS 'parallel_to(平行分支)/derived_from(衍生)/crosses_over_with(跨界融合)
JP parallel_to(並行分岐)/derived_from(派生)/crosses_over_with(クロスオーバー)
EN parallel_to/parallel/derived_from/derived/crosses_over_with/crossover';

CREATE INDEX idx_univ_rel_project ON universe_relations (project_id);
CREATE INDEX idx_univ_rel_from ON universe_relations (from_universe_id);
CREATE INDEX idx_univ_rel_to ON universe_relations (to_universe_id);

-- ════════════════════════════════════════════════════════════════════════════
-- 现有表加 universe_id
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE timelines
    ADD COLUMN IF NOT EXISTS universe_id UUID REFERENCES universes(id);
CREATE INDEX IF NOT EXISTS idx_timelines_universe ON timelines (universe_id);

ALTER TABLE character_profiles
    ADD COLUMN IF NOT EXISTS universe_id UUID REFERENCES universes(id);
CREATE INDEX IF NOT EXISTS idx_profiles_universe ON character_profiles (universe_id);

ALTER TABLE canon_events
    ADD COLUMN IF NOT EXISTS universe_id UUID REFERENCES universes(id);
CREATE INDEX IF NOT EXISTS idx_canon_event_universe ON canon_events (universe_id);

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS universe_id UUID REFERENCES universes(id);
CREATE INDEX IF NOT EXISTS idx_locations_universe ON locations (universe_id);
