# Novel Weaver Gateway

Spring Boot 4.x MCP Gateway · **45 tools** · PostgreSQL + pgvector + ArcadeDB + Meilisearch + LanguageTool

---

## 中文

小说写作辅助系统的数据后端 — 通过 MCP 提供结构化数据存储和检索，让 AI 记住你写过的一切。

这不是"AI 写小说"工具。它提供结构化数据，让 AI 能记住人物状态、关系变化、时间线、伏笔、地点变更。创作决策永远由你来做。

### 快速启动

```bash
# 1. 启动基础设施 (PG + ArcadeDB + Meilisearch + LanguageTool)
docker compose -f infra.yml up -d

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填入你的配置

# 3. 构建并启动 Gateway
docker compose up -d --build

# 4. 验证
curl http://localhost:8883/health
# → {"status":"UP","postgresql":"UP","arcadedb":"UP"}

# 5. 列出所有工具
curl -s -X POST http://localhost:8883/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

### 客户端配置

| 平台 | 配置方式 |
|------|----------|
| **Reasonix** | 复制 `agents/reasonix/` → `.reasonix/skills/` |
| **Claude Code** | 合并 `agents/claude/settings-template.json` → `.claude/settings.local.json` |
| **Claude Desktop** | 合并 `agents/claude/claude_desktop_config.template.json` → `claude_desktop_config.json` |

配置完成后，告诉 AI：**"初始化项目"**

### 45 个 MCP 工具

```
项目管理:    project_init / project_archive / project_delete / service_reset
            project_export / project_import
章节:        chapter_sync / chapter_list / chapter_get
角色:        character_save / character_status / character_snapshot
            character_snapshot_check
物品:        item_register / item_update / item_query
地点:        location_register / location_update / location_status
世界:        universe_create / universe_list / universe_link
搜索:        fuzzy_search / rag_search / semantic_search
关系图谱:    graph_query / graph_path
时间线:      timeline_create / timeline_event_add / timeline_event_update
            timeline_check / timeline_link_create / timeline_link_query
正典:        canon_import / canon_character_add / canon_event_add
            canon_relationship_add / canon_search / canon_verify
            canon_status_set
伏笔:        register_foreshadowing
推演:        deduce_outline / deduce_behavior / deduce_verify
语法检查:    grammar_check
```

### 目录结构

```
gateway/
├── infra.yml              ← 基础设施 (PG+ArcadeDB+Meilisearch+LanguageTool)
├── docker-compose.yml     ← Gateway 服务
├── agents/                ← 客户端配置 (Reasonix / Claude)
│   └── templates/         ← 初始化向导模板
├── docs/                  ← 设计文档
└── src/                   ← Java 源码 + Flyway 迁移
```

### 未来计划 (MCP 跑通后)

**Phase 1 — 当前**：MCP 工具链跑通，Markdown 本地 + PG 结构化 + ArcadeDB 图关系 三同步

- [x] 45 个 MCP 工具覆盖项目管理/章节/角色/物品/地点/时间线/正典/推演
- [x] ArcadeDB 替代 Neo4j 做图存储（物理租户，每项目独立数据库）
- [x] Agent 指令（Reasonix + Claude）写作流程自动化

**Phase 2 — 同名实体区分** ✅

- [x] `identity` JSONB 字段已在 V1 中，利用它支持同名角色/物品/地点（如不同宇宙/平行世界的同名角色）
- [x] 同名时返回列表让 AI 根据 `identity` 上下文判断

**Phase 3 — 本地文件 ↔ PG 双向恢复**

- [ ] PG 的 `chapters.content` 已存正文全文，补充存储 `characters/*.md`、`items/*.md`、`locations/*.md` 的全文
- [ ] `project_restore_files` 工具：从 PG 恢复全部本地 markdown 文件
- [ ] `project_sync_from_files` 工具：扫描本地目录，全量同步到 PG + ArcadeDB
- [ ] 实现"本地丢了从 PG 恢复，PG 坏了从本地重建"的双向兜底

**Phase 4 — Web UI**

- [ ] 基于 PG 的 JPA 生态搭建 Web 管理界面
- [ ] 网页端直接编辑角色/物品/地点/章节，写入 PG 并触发 ArcadeDB 图同步

**Phase 5 — 评估 PG → ArcadeDB 融合**

- [ ] 将 PG 的结构化数据迁到 ArcadeDB Document 模型，PG 转为纯 markdown 全文备份仓库
- [ ] 目标架构：**Markdown (源) → ArcadeDB (结构+图+向量) → PG (全文备份)**

### 许可证

MIT

---

## 日本語

小説執筆支援システムのデータバックエンド — MCP経由で構造化データを保存・検索し、AIにあなたの執筆内容を記憶させます。

これは「AIが小説を書く」ツールではありません。構造化データを提供し、AIがキャラクターの状態、関係の変化、タイムライン、伏線、場所の変更を記憶できるようにします。創作の決定は常にあなたが行います。

### クイックスタート

```bash
# 1. インフラを起動 (PG + ArcadeDB + Meilisearch + LanguageTool)
docker compose -f infra.yml up -d

# 2. 環境変数を設定
cp .env.example .env
# .env を編集して設定を入力

# 3. Gateway をビルド＆起動
docker compose up -d --build

# 4. 確認
curl http://localhost:8883/health
# → ok

# 5. 全ツールを一覧表示
curl -s -X POST http://localhost:8883/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

### クライアント設定

| プラットフォーム | 設定方法 |
|------------------|----------|
| **Reasonix** | `agents/reasonix/` をコピー → `.reasonix/skills/` |
| **Claude Code** | `agents/claude/settings-template.json` をマージ → `.claude/settings.local.json` |
| **Claude Desktop** | `agents/claude/claude_desktop_config.template.json` をマージ → `claude_desktop_config.json` |

設定後、AI に伝えてください：**"プロジェクトを初期化"**

### 33 MCP ツール

```
プロジェクト管理: project_init / project_archive / project_delete / service_reset
                 project_export / project_import
チャプター:      chapter_sync / chapter_list / chapter_get
キャラクター:    character_save / character_status / character_snapshot
                 character_snapshot_check
ロケーション:    location_register / location_update / location_status
ユニバース:      universe_create / universe_list / universe_link
検索:            fuzzy_search / rag_search / semantic_search
グラフ:          graph_query / graph_path
タイムライン:    timeline_create / timeline_event_add / timeline_check
                 timeline_link_create / timeline_link_query
カノン:          canon_import / canon_search / canon_verify / canon_status_set
伏線:            register_foreshadowing
推論:            deduce_outline / deduce_behavior / deduce_verify
```

### ディレクトリ構成

```
gateway/
├── infra.yml              ← インフラ (PG+ArcadeDB+Meilisearch+LanguageTool)
├── docker-compose.yml     ← Gateway サービス
├── agents/                ← クライアント設定 (Reasonix / Claude)
│   └── templates/         ← 初期化ウィザード用テンプレート
├── docs/                  ← 設計ドキュメント
└── src/                   ← Java ソース + Flyway マイグレーション
```

### 今後の予定 (MCP 安定稼働後)

**Phase 1 — 現在**：MCP ツールチェーン稼働、Markdown ローカル + PG 構造化 + ArcadeDB グラフ 三同期

- [x] 45 MCP ツール（プロジェクト/章/キャラクター/アイテム/場所/タイムライン/正典/推論）
- [x] ArcadeDB が Neo4j を置き換え（物理テナント、プロジェクト毎に独立 DB）
- [x] Agent 指示書（Reasonix + Claude）執筆フロー自動化

**Phase 2 — 同名エンティティ区別** ✅

- [x] `identity` JSONB カラムで同名キャラ/アイテム/場所を区別（例：異なる宇宙の同名キャラ）
- [x] 同名時にリストを返し、AI が `identity` コンテキストで判断

**Phase 3 — ローカル ↔ PG 双方向復元**

- [ ] PG に markdown 全文を保存（キャラ/アイテム/場所の `source_md` カラム）
- [ ] `project_restore_files`：PG からローカル markdown を全復元
- [ ] `project_sync_from_files`：ローカルディレクトリをスキャンして PG + ArcadeDB に全同期
  - [ ]「ローカル紛失 → PG から復元」「PG 破損 → ローカルから再構築」を実現

**Phase 4 — Web UI**

- [ ] PG の JPA エコシステムで Web 管理画面を構築
- [ ] ブラウザから直接編集、PG に保存 + ArcadeDB グラフ同期

**Phase 5 — PG → ArcadeDB 統合評価**

- [ ] PG の構造化データを ArcadeDB Document モデルに移行、PG は markdown 全文バックアップに
- [ ] 目標アーキテクチャ：**Markdown (源) → ArcadeDB (構造+グラフ+ベクトル) → PG (全文バックアップ)**

### ライセンス

MIT

---

## English

A data backend for novel writing — provides structured storage & retrieval via MCP, so AI remembers everything you've written.

This is NOT an "AI writes your novel" tool. It provides structured data so AI can remember character states, relationships, timelines, foreshadowing, and location changes. Creative decisions are always yours.

### Quick Start

```bash
# 1. Start infrastructure (PG + ArcadeDB + Meilisearch + LanguageTool)
docker compose -f infra.yml up -d

# 2. Configure environment
cp .env.example .env
# edit .env with your settings

# 3. Build & start Gateway
docker compose up -d --build

# 4. Verify
curl http://localhost:8883/health
# → ok

# 5. List all tools
curl -s -X POST http://localhost:8883/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

### Client Setup

| Platform | How to |
|----------|--------|
| **Reasonix** | Copy `agents/reasonix/` → `.reasonix/skills/` |
| **Claude Code** | Merge `agents/claude/settings-template.json` → `.claude/settings.local.json` |
| **Claude Desktop** | Merge `agents/claude/claude_desktop_config.template.json` → `claude_desktop_config.json` |

After setup, tell your AI: **"initialize project"**

### 33 MCP Tools

```
Project Mgmt:   project_init / project_archive / project_delete / service_reset
                project_export / project_import
Chapter:        chapter_sync / chapter_list / chapter_get
Character:      character_save / character_status / character_snapshot
                character_snapshot_check
Location:       location_register / location_update / location_status
Universe:       universe_create / universe_list / universe_link
Search:         fuzzy_search / rag_search / semantic_search
Graph:          graph_query / graph_path
Timeline:       timeline_create / timeline_event_add / timeline_check
                timeline_link_create / timeline_link_query
Canon:          canon_import / canon_search / canon_verify / canon_status_set
Foreshadowing:  register_foreshadowing
Deduction:      deduce_outline / deduce_behavior / deduce_verify
```

### Directory Structure

```
gateway/
├── infra.yml              ← Infrastructure (PG+ArcadeDB+Meilisearch+LanguageTool)
├── docker-compose.yml     ← Gateway service
├── agents/                ← Client configs (Reasonix / Claude)
│   └── templates/         ← Project templates for init wizard
├── docs/                  ← Design documents
└── src/                   ← Java source + Flyway migration
```

### Future Plans (post MCP stabilization)

**Phase 1 — Current**：MCP toolchain stable, Markdown + PG + ArcadeDB tri-sync

- [x] 45 MCP tools across projects/chapters/characters/items/locations/timelines/canon/deduction
- [x] ArcadeDB replaced Neo4j for graph storage (physical tenant, per-project DB)
- [x] Agent instructions (Reasonix + Claude) for automated writing workflows

**Phase 2 — Same-name entity disambiguation** ✅

- [x] `identity` JSONB column ready, supports same-name characters/items/locations (e.g. same-name characters across
  different universes)
- [x] Return list on same-name conflict, AI resolves via `identity` context

**Phase 3 — Local files ↔ PG bidirectional recovery**

- [ ] Store full markdown content in PG (`source_md` columns for characters/items/locations)
- [ ] `project_restore_files`：restore all local markdown files from PG
- [ ] `project_sync_from_files`：scan local dir, full sync to PG + ArcadeDB
- [ ] Bidirectional safety net: local loss → PG recovery; PG failure → local rebuild

**Phase 4 — Web UI**

- [ ] Web admin UI built on PG's JPA ecosystem
- [ ] In-browser editing of characters/items/locations/chapters, syncing to PG + ArcadeDB

**Phase 5 — Evaluate PG → ArcadeDB consolidation**

- [ ] Migrate PG structured data to ArcadeDB Document model, PG becomes pure markdown backup
- [ ] Target architecture: **Markdown (source) → ArcadeDB (structure+graph+vector) → PG (full-text backup)**

### License

MIT
