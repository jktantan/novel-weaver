# Novel Weaver Gateway

Spring Boot 4.x MCP Gateway · 33 tools · PostgreSQL + pgvector + Neo4j + Meilisearch + LanguageTool

---

## 中文

小说写作辅助系统的数据后端 — 通过 MCP 提供结构化数据存储和检索，让 AI 记住你写过的一切。

这不是"AI 写小说"工具。它提供结构化数据，让 AI 能记住人物状态、关系变化、时间线、伏笔、地点变更。创作决策永远由你来做。

### 快速启动

```bash
# 1. 启动基础设施 (PG + Neo4j + Meilisearch + LanguageTool)
docker compose -f infra.yml up -d

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填入你的配置

# 3. 构建并启动 Gateway
docker compose up -d --build

# 4. 验证
curl http://localhost:8883/health
# → ok

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

### 33 个 MCP 工具

```
项目管理:    project_init / project_archive / project_delete / service_reset
            project_export / project_import
章节:        chapter_sync / chapter_list / chapter_get
角色:        character_save / character_status / character_snapshot
            character_snapshot_check
地点:        location_register / location_update / location_status
世界:        universe_create / universe_list / universe_link
搜索:        fuzzy_search / rag_search / semantic_search
关系图谱:    graph_query / graph_path
时间线:      timeline_create / timeline_event_add / timeline_check
            timeline_link_create / timeline_link_query
正典:        canon_import / canon_search / canon_verify / canon_status_set
伏笔:        register_foreshadowing
推演:        deduce_outline / deduce_behavior / deduce_verify
```

### 目录结构

```
gateway/
├── infra.yml              ← 基础设施 (PG+Neo4j+Meilisearch+LanguageTool)
├── docker-compose.yml     ← Gateway 服务
├── agents/                ← 客户端配置 (Reasonix / Claude)
│   └── templates/         ← 初始化向导模板
├── docs/                  ← 设计文档
└── src/                   ← Java 源码 + Flyway 迁移
```

### 许可证

MIT

---

## 日本語

小説執筆支援システムのデータバックエンド — MCP経由で構造化データを保存・検索し、AIにあなたの執筆内容を記憶させます。

これは「AIが小説を書く」ツールではありません。構造化データを提供し、AIがキャラクターの状態、関係の変化、タイムライン、伏線、場所の変更を記憶できるようにします。創作の決定は常にあなたが行います。

### クイックスタート

```bash
# 1. インフラを起動 (PG + Neo4j + Meilisearch + LanguageTool)
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
├── infra.yml              ← インフラ (PG+Neo4j+Meilisearch+LanguageTool)
├── docker-compose.yml     ← Gateway サービス
├── agents/                ← クライアント設定 (Reasonix / Claude)
│   └── templates/         ← 初期化ウィザード用テンプレート
├── docs/                  ← 設計ドキュメント
└── src/                   ← Java ソース + Flyway マイグレーション
```

### ライセンス

MIT

---

## English

A data backend for novel writing — provides structured storage & retrieval via MCP, so AI remembers everything you've written.

This is NOT an "AI writes your novel" tool. It provides structured data so AI can remember character states, relationships, timelines, foreshadowing, and location changes. Creative decisions are always yours.

### Quick Start

```bash
# 1. Start infrastructure (PG + Neo4j + Meilisearch + LanguageTool)
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
├── infra.yml              ← Infrastructure (PG+Neo4j+Meilisearch+LanguageTool)
├── docker-compose.yml     ← Gateway service
├── agents/                ← Client configs (Reasonix / Claude)
│   └── templates/         ← Project templates for init wizard
├── docs/                  ← Design documents
└── src/                   ← Java source + Flyway migration
```

### License

MIT
