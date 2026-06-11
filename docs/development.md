# Novel Weaver Gateway — 开发文档 / 開発ドキュメント / Development Guide

> **CN** 面向开发者。读完就能直接写代码。  
> **JP** 開発者向け。読めばすぐにコードを書ける。  
> **EN** For developers. Read and write code immediately.

---

## 中文

### 1. 是什么

一个 Spring Boot 4.x 应用。做了两件事：

1. **MCP Streamable HTTP 端点** — `POST /mcp`，给 AI 客户端调用
2. **所有数据层查询/写入的编排** — PG + pgvector + Neo4j + Meilisearch + LanguageTool

Gateway 不调任何外部 LLM。推演和写作在客户端侧完成。

---

### 2. 项目结构（全部 26 个源文件）

```
gateway/src/main/java/com/novelweaver/
├── GatewayApplication.java              ← main()
├── config/
│   ├── HealthController.java            ← GET /health（Docker 健康检查）
│   └── WebClientConfig.java             ← WebClient.Builder Bean
├── model/                               ← 每个表一个 entity
│   ├── Project.java                     ← projects
│   ├── Chapter.java                     ← chapters
│   ├── ChapterVersion.java              ← chapter_versions
│   ├── ChapterParagraph.java            ← chapter_paragraphs
│   ├── CharacterProfile.java            ← character_profiles
│   ├── CharacterSnapshot.java           ← character_snapshots
│   ├── CharacterVoiceprint.java         ← character_voiceprints
│   ├── CharacterRelationship.java       ← character_relationships
│   ├── Foreshadowing.java               ← foreshadowing_index
│   ├── DeductionLog.java                ← deduction_logs
│   ├── Timeline.java                    ← timelines
│   ├── TimelineEvent.java               ← timeline_events
│   ├── Location.java                    ← locations
│   ├── CanonSource.java                 ← canon_sources
│   ├── CanonCharacter.java              ← canon_characters
│   ├── CanonEvent.java                  ← canon_events
│   └── CanonRelationship.java           ← canon_relationships
├── repository/                          ← 每个 entity 一个 repository
│   ├── ProjectRepository.java
│   ├── ChapterRepository.java
│   ├── ChapterVersionRepository.java
│   ├── ChapterParagraphRepository.java
│   ├── CharacterProfileRepository.java
│   ├── CharacterSnapshotRepository.java
│   ├── CharacterVoiceprintRepository.java
│   ├── CharacterRelationshipRepository.java
│   ├── ForeshadowingRepository.java
│   ├── DeductionLogRepository.java
│   ├── TimelineRepository.java
│   ├── TimelineEventRepository.java
│   ├── LocationRepository.java
│   ├── CanonSourceRepository.java
│   ├── CanonCharacterRepository.java
│   ├── CanonEventRepository.java
│   └── CanonRelationshipRepository.java
├── service/                             ← 每个 service 一组 @McpTool
│   ├── ProjectService.java              ← 6 tools
│   ├── ChapterService.java              ← 3 tools
│   ├── CharacterService.java            ← 3 tools
│   ├── LocationService.java             ← 3 tools
│   ├── RAGService.java                  ← 3 tools
│   ├── GraphService.java                ← 2 tools
│   ├── DeductionService.java            ← 4 tools
│   ├── CanonService.java                ← 3 tools
│   ├── TimelineService.java             ← 3 tools
│   └── (ReviewService 已废弃)            ← 审查由客户端 AI 完成
└── type/
    └── PgVectorType.java                ← Hibernate UserType: String ↔ pgvector

gateway/src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__schema.sql                   ← 初始 16 张表
    ├── V2__add_missing_tables.sql       ← 补充表
    └── V3__add_locations.sql            ← locations 表
```

**没有独立的 controller 层。** MCP 端点由 Spring AI 自动提供，工具通过 `@McpTool` 注解自动注册。

---

### 3. 技术栈

| 层 | 选型 |
|----|------|
| Framework | Spring Boot 4.0.6 · Java 21 |
| MCP | STATELESS（无 session） |
| DB Migration | Flyway（`ddl-auto: validate`） |
| DB | PostgreSQL 16+ · pgvector |
| Graph | Neo4j 5 Community（直接 Cypher） |
| Search | Meilisearch（HTTP，无 SDK） |
| Syntax | LanguageTool（HTTP POST） |
| Boilerplate | Lombok（entity 用 `@Getter @Setter`） |

---

### 4. 数据库（17 张表）

所有 DDL 见 `db/migration/V1__schema.sql` + V2 + V3。

#### 核心表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `projects` | 项目元信息 | name, type(original/fanfic), status, meta(jsonb) |
| `chapters` | 章节正文+元数据 | chapter_number, title, content, phase, word_count, status |
| `chapter_versions` | 每次保存的版本 | chapter_id(FK), version, content, word_count |
| `chapter_paragraphs` | 段落级向量索引 | content, embedding(vector(1024)), scene, scene_type, pov_character |
| `character_profiles` | 角色画像 | name, bio, traits(jsonb), voice, voice_meta(jsonb) |
| `character_snapshots` | 每章后状态快照 | character_id(FK), chapter_id(FK), location, physical, psychology |
| `locations` | 地点档案管理 | name, type, region, canon_description, change_log(jsonb), current_status |

#### 关系/伏笔/声纹

| 表 | 用途 |
|----|------|
| `character_relationships` | 人物关系 |
| `character_voiceprints` | 声纹样本 |
| `foreshadowing_index` | 伏笔登记 |
| `deduction_logs` | 推演日志 |

#### 时间线

| 表 | 用途 |
|----|------|
| `timelines` | 时间线定义 |
| `timeline_events` | 时间线事件（含 is_canon 区分正典/原创） |

#### 正典（仅同人项目）

| 表 | 用途 |
|----|------|
| `canon_sources` | 正典来源 |
| `canon_characters` | 正典人物 |
| `canon_events` | 正典事件 |
| `canon_relationships` | 正典关系 |

#### 多项目隔离

所有表都带 `project_id`。唯一约束通常是 `(project_id, name)` 或 `(project_id, chapter_number)`。

---

### 5. MCP 端点

| 方法 | 路径 | 用途 |
|------|------|------|
| `POST` | `/mcp` | JSON-RPC 请求（tools/list、tools/call） |
| `GET` | `/health` | 健康检查 |

配置在 `application.yml`：
```yaml
spring.ai.mcp.server:
  protocol: STATELESS   # 无 session，重启不丢连接
  type: SYNC
```

---

### 6. 30 个 MCP 工具

| 分类 | 工具 | Service |
|------|------|---------|
| **项目管理** | `project_init` | ProjectService |
| | `project_archive` | ProjectService |
| | `project_delete` | ProjectService |
| | `service_reset` | ProjectService |
| | `project_export` | ProjectService |
| | `project_import` | ProjectService |
| **章节** | `chapter_sync` | ChapterService |
| | `chapter_get` | ChapterService |
| | `chapter_list` | ChapterService |
| **人物** | `character_save` | CharacterService |
| | `character_status` | CharacterService |
| | `character_snapshot` | CharacterService |
| **地点** | `location_register` | LocationService |
| | `location_update` | LocationService |
| | `location_status` | LocationService |
| **搜索** | `rag_search` | RAGService |
| | `semantic_search` | RAGService |
| | `fuzzy_search` | RAGService |
| **图谱** | `graph_query` | GraphService |
| | `graph_path` | GraphService |
| **时间线** | `timeline_create` | TimelineService |
| | `timeline_event_add` | TimelineService |
| | `timeline_check` | TimelineService |
| **推演** | `deduce_behavior` | DeductionService |
| | `deduce_outline` | DeductionService |
| | `deduce_verify` | DeductionService |
| | `register_foreshadowing` | DeductionService |
| **正典** | `canon_import` | CanonService |
| | `canon_search` | CanonService |
| | `canon_verify` | CanonService |

**注意**：`review` 工具已废弃。审查由客户端 AI agent 完成。

---

### 7. 新增一个工具的步骤

1. 在对应 Service 类中加方法，用 `@McpTool` 和 `@McpToolParam` 注解
2. 返回类型用 `record`（框架自动序列化）
3. 重启 Gateway — 即刻生效。不需要任何注册代码

---

### 8. 外部服务

| 服务 | 接入方式 |
|------|---------|
| **Meilisearch** | 直接 HTTP（WebClient），索引名 `novel_chapters` |
| **LanguageTool** | HTTP POST `/v2/check`（form-encoded） |
| **Neo4j** | `Neo4jClient` 直接执行 Cypher，无 @Node 实体 |
| **Ollama (bge-m3)** | 客户端侧调用，Gateway 只存向量到 pgvector |

---

### 9. pgvector 注意事项

- 向量列声明为 `String`，`@Type(PgVectorType.class)` 处理映射
- 向量搜索 `<->` 操作符只能用 `nativeQuery=true`
- 向量格式为 `[0.123, 0.456, ...]` 字符串

---

## 日本語

### 1. 概要

Spring Boot 4.x アプリケーション。二つのことを行う：

1. **MCP Streamable HTTP エンドポイント** — `POST /mcp`、AI クライアントから呼び出し
2. **全データ層のクエリ/書き込みのオーケストレーション** — PG + pgvector + Neo4j + Meilisearch + LanguageTool

Gateway は外部 LLM を呼び出さない。推論と執筆はクライアント側で完了。

---

### 2. プロジェクト構成（全 26 ソースファイル）

```
gateway/src/main/java/com/novelweaver/
├── GatewayApplication.java              ← main()
├── config/
│   ├── HealthController.java            ← GET /health（Docker ヘルスチェック）
│   └── WebClientConfig.java             ← WebClient.Builder Bean
├── model/                               ← テーブルごとに一つの entity
│   ├── Project.java                     ← projects
│   ├── Chapter.java                     ← chapters
│   ├── ChapterVersion.java              ← chapter_versions
│   ├── ChapterParagraph.java            ← chapter_paragraphs
│   ├── CharacterProfile.java            ← character_profiles
│   ├── CharacterSnapshot.java           ← character_snapshots
│   ├── CharacterVoiceprint.java         ← character_voiceprints
│   ├── CharacterRelationship.java       ← character_relationships
│   ├── Foreshadowing.java               ← foreshadowing_index
│   ├── DeductionLog.java                ← deduction_logs
│   ├── Timeline.java                    ← timelines
│   ├── TimelineEvent.java               ← timeline_events
│   ├── Location.java                    ← locations
│   ├── CanonSource.java                 ← canon_sources
│   ├── CanonCharacter.java              ← canon_characters
│   ├── CanonEvent.java                  ← canon_events
│   └── CanonRelationship.java           ← canon_relationships
├── repository/                          ← entity ごとに一つの repository
│   ├── ProjectRepository.java
│   ├── ChapterRepository.java
│   ├── ChapterVersionRepository.java
│   ├── ChapterParagraphRepository.java
│   ├── CharacterProfileRepository.java
│   ├── CharacterSnapshotRepository.java
│   ├── CharacterVoiceprintRepository.java
│   ├── CharacterRelationshipRepository.java
│   ├── ForeshadowingRepository.java
│   ├── DeductionLogRepository.java
│   ├── TimelineRepository.java
│   ├── TimelineEventRepository.java
│   ├── LocationRepository.java
│   ├── CanonSourceRepository.java
│   ├── CanonCharacterRepository.java
│   ├── CanonEventRepository.java
│   └── CanonRelationshipRepository.java
├── service/                             ← service ごとに @McpTool グループ
│   ├── ProjectService.java              ← 6 tools
│   ├── ChapterService.java              ← 3 tools
│   ├── CharacterService.java            ← 3 tools
│   ├── LocationService.java             ← 3 tools
│   ├── RAGService.java                  ← 3 tools
│   ├── GraphService.java                ← 2 tools
│   ├── DeductionService.java            ← 4 tools
│   ├── CanonService.java                ← 3 tools
│   ├── TimelineService.java             ← 3 tools
│   └── (ReviewService 廃止)             ← レビューはクライアント AI が担当
└── type/
    └── PgVectorType.java                ← Hibernate UserType: String ↔ pgvector

gateway/src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__schema.sql                   ← 初期 16 テーブル
    ├── V2__add_missing_tables.sql       ← 補完テーブル
    └── V3__add_locations.sql            ← locations テーブル
```

**独立した controller 層はない。** MCP エンドポイントは Spring AI が自動提供し、ツールは `@McpTool` アノテーションで自動登録される。

---

### 3. 技術スタック

| 層 | 選定 |
|----|------|
| Framework | Spring Boot 4.0.6 · Java 21 |
| MCP | STATELESS（セッションなし） |
| DB Migration | Flyway（`ddl-auto: validate`） |
| DB | PostgreSQL 16+ · pgvector |
| Graph | Neo4j 5 Community（直接 Cypher） |
| Search | Meilisearch（HTTP、SDK なし） |
| Syntax | LanguageTool（HTTP POST） |
| Boilerplate | Lombok（entity は `@Getter @Setter`） |

---

### 4. データベース（17 テーブル）

全 DDL は `db/migration/V1__schema.sql` + V2 + V3 を参照。

#### コアテーブル

| テーブル | 用途 | キーフィールド |
|----------|------|---------------|
| `projects` | プロジェクトメタ情報 | name, type(original/fanfic), status, meta(jsonb) |
| `chapters` | チャプター本文＋メタデータ | chapter_number, title, content, phase, word_count, status |
| `chapter_versions` | 保存ごとのバージョン | chapter_id(FK), version, content, word_count |
| `chapter_paragraphs` | 段落レベルベクトル索引 | content, embedding(vector(1024)), scene, scene_type, pov_character |
| `character_profiles` | キャラクター設定 | name, bio, traits(jsonb), voice, voice_meta(jsonb) |
| `character_snapshots` | 章ごとの状態スナップショット | character_id(FK), chapter_id(FK), location, physical, psychology |
| `locations` | ロケーション管理 | name, type, region, canon_description, change_log(jsonb), current_status |

#### 関係/伏線/声紋

| テーブル | 用途 |
|----------|------|
| `character_relationships` | キャラクター関係 |
| `character_voiceprints` | 声紋サンプル |
| `foreshadowing_index` | 伏線管理 |
| `deduction_logs` | 推論ログ |

#### タイムライン

| テーブル | 用途 |
|----------|------|
| `timelines` | タイムライン定義 |
| `timeline_events` | タイムラインイベント（is_canon で正典/原創を区別） |

#### 正典（二次創作のみ）

| テーブル | 用途 |
|----------|------|
| `canon_sources` | 正典ソース |
| `canon_characters` | 正典キャラクター |
| `canon_events` | 正典イベント |
| `canon_relationships` | 正典関係 |

#### マルチプロジェクト分離

全テーブルに `project_id` あり。ユニーク制約は通常 `(project_id, name)` または `(project_id, chapter_number)`。

---

### 5. MCP エンドポイント

| メソッド | パス | 用途 |
|----------|------|------|
| `POST` | `/mcp` | JSON-RPC リクエスト（tools/list、tools/call） |
| `GET` | `/health` | ヘルスチェック |

`application.yml` での設定：
```yaml
spring.ai.mcp.server:
  protocol: STATELESS   # セッションなし、再起動で接続断にならない
  type: SYNC
```

---

### 6. 30 MCP ツール

| 分類 | ツール | Service |
|------|--------|---------|
| **プロジェクト管理** | `project_init` | ProjectService |
| | `project_archive` | ProjectService |
| | `project_delete` | ProjectService |
| | `service_reset` | ProjectService |
| | `project_export` | ProjectService |
| | `project_import` | ProjectService |
| **チャプター** | `chapter_sync` | ChapterService |
| | `chapter_get` | ChapterService |
| | `chapter_list` | ChapterService |
| **キャラクター** | `character_save` | CharacterService |
| | `character_status` | CharacterService |
| | `character_snapshot` | CharacterService |
| **ロケーション** | `location_register` | LocationService |
| | `location_update` | LocationService |
| | `location_status` | LocationService |
| **検索** | `rag_search` | RAGService |
| | `semantic_search` | RAGService |
| | `fuzzy_search` | RAGService |
| **グラフ** | `graph_query` | GraphService |
| | `graph_path` | GraphService |
| **タイムライン** | `timeline_create` | TimelineService |
| | `timeline_event_add` | TimelineService |
| | `timeline_check` | TimelineService |
| **推論** | `deduce_behavior` | DeductionService |
| | `deduce_outline` | DeductionService |
| | `deduce_verify` | DeductionService |
| | `register_foreshadowing` | DeductionService |
| **正典** | `canon_import` | CanonService |
| | `canon_search` | CanonService |
| | `canon_verify` | CanonService |

**注意**：`review` ツールは廃止。レビューはクライアント AI エージェントが行う。

---

### 7. 新規ツール追加手順

1. 該当 Service クラスに `@McpTool` と `@McpToolParam` アノテーション付きメソッドを追加
2. 戻り値の型は `record` を使用（フレームワークが自動シリアライズ）
3. Gateway を再起動——即座に反映される。登録コードは不要

---

### 8. 外部サービス

| サービス | 接続方式 |
|----------|---------|
| **Meilisearch** | 直接 HTTP（WebClient）、索引名 `novel_chapters` |
| **LanguageTool** | HTTP POST `/v2/check`（form-encoded） |
| **Neo4j** | `Neo4jClient` で直接 Cypher 実行、@Node entity なし |
| **Ollama (bge-m3)** | クライアント側で呼び出し、Gateway はベクトルを pgvector に保存のみ |

---

### 9. pgvector 注意事項

- ベクトル列は `String` として宣言、`@Type(PgVectorType.class)` でマッピング
- ベクトル検索 `<->` 演算子は `nativeQuery=true` のみ使用可能
- ベクトル形式： `[0.123, 0.456, ...]` 文字列

---

## English

### 1. What It Is

A Spring Boot 4.x application that does two things:

1. **MCP Streamable HTTP endpoint** — `POST /mcp`, for AI clients to call
2. **Orchestrates all data-layer queries/writes** — PG + pgvector + Neo4j + Meilisearch + LanguageTool

Gateway does NOT call any external LLM. Deduction and writing happen on the client side.

---

### 2. Project Structure (26 source files total)

```
gateway/src/main/java/com/novelweaver/
├── GatewayApplication.java              ← main()
├── config/
│   ├── HealthController.java            ← GET /health (Docker health check)
│   └── WebClientConfig.java             ← WebClient.Builder Bean
├── model/                               ← One entity per table
│   ├── Project.java                     ← projects
│   ├── Chapter.java                     ← chapters
│   ├── ChapterVersion.java              ← chapter_versions
│   ├── ChapterParagraph.java            ← chapter_paragraphs
│   ├── CharacterProfile.java            ← character_profiles
│   ├── CharacterSnapshot.java           ← character_snapshots
│   ├── CharacterVoiceprint.java         ← character_voiceprints
│   ├── CharacterRelationship.java       ← character_relationships
│   ├── Foreshadowing.java               ← foreshadowing_index
│   ├── DeductionLog.java                ← deduction_logs
│   ├── Timeline.java                    ← timelines
│   ├── TimelineEvent.java               ← timeline_events
│   ├── Location.java                    ← locations
│   ├── CanonSource.java                 ← canon_sources
│   ├── CanonCharacter.java              ← canon_characters
│   ├── CanonEvent.java                  ← canon_events
│   └── CanonRelationship.java           ← canon_relationships
├── repository/                          ← One repository per entity
│   ├── ProjectRepository.java
│   ├── ChapterRepository.java
│   ├── ChapterVersionRepository.java
│   ├── ChapterParagraphRepository.java
│   ├── CharacterProfileRepository.java
│   ├── CharacterSnapshotRepository.java
│   ├── CharacterVoiceprintRepository.java
│   ├── CharacterRelationshipRepository.java
│   ├── ForeshadowingRepository.java
│   ├── DeductionLogRepository.java
│   ├── TimelineRepository.java
│   ├── TimelineEventRepository.java
│   ├── LocationRepository.java
│   ├── CanonSourceRepository.java
│   ├── CanonCharacterRepository.java
│   ├── CanonEventRepository.java
│   └── CanonRelationshipRepository.java
├── service/                             ← Each service is a @McpTool group
│   ├── ProjectService.java              ← 6 tools
│   ├── ChapterService.java              ← 3 tools
│   ├── CharacterService.java            ← 3 tools
│   ├── LocationService.java             ← 3 tools
│   ├── RAGService.java                  ← 3 tools
│   ├── GraphService.java                ← 2 tools
│   ├── DeductionService.java            ← 4 tools
│   ├── CanonService.java                ← 3 tools
│   ├── TimelineService.java             ← 3 tools
│   └── (ReviewService deprecated)       ← Review handled by client AI
└── type/
    └── PgVectorType.java                ← Hibernate UserType: String ↔ pgvector

gateway/src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__schema.sql                   ← Initial 16 tables
    ├── V2__add_missing_tables.sql       ← Supplementary tables
    └── V3__add_locations.sql            ← locations table
```

**There is no separate controller layer.** MCP endpoints are auto-provided by Spring AI, and tools are auto-registered via the `@McpTool` annotation.

---

### 3. Tech Stack

| Layer | Choice |
|-------|--------|
| Framework | Spring Boot 4.0.6 · Java 21 |
| MCP | STATELESS (no sessions) |
| DB Migration | Flyway (`ddl-auto: validate`) |
| DB | PostgreSQL 16+ · pgvector |
| Graph | Neo4j 5 Community (direct Cypher) |
| Search | Meilisearch (HTTP, no SDK) |
| Syntax | LanguageTool (HTTP POST) |
| Boilerplate | Lombok (`@Getter @Setter` on entities) |

---

### 4. Database (17 tables)

All DDL in `db/migration/V1__schema.sql` + V2 + V3.

#### Core Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `projects` | Project metadata | name, type(original/fanfic), status, meta(jsonb) |
| `chapters` | Chapter body + metadata | chapter_number, title, content, phase, word_count, status |
| `chapter_versions` | Version history per save | chapter_id(FK), version, content, word_count |
| `chapter_paragraphs` | Paragraph-level vector index | content, embedding(vector(1024)), scene, scene_type, pov_character |
| `character_profiles` | Character profiles | name, bio, traits(jsonb), voice, voice_meta(jsonb) |
| `character_snapshots` | Per-chapter state snapshots | character_id(FK), chapter_id(FK), location, physical, psychology |
| `locations` | Location registry | name, type, region, canon_description, change_log(jsonb), current_status |

#### Relationships / Foreshadowing / Voiceprints

| Table | Purpose |
|-------|---------|
| `character_relationships` | Character relationships |
| `character_voiceprints` | Voiceprint samples |
| `foreshadowing_index` | Foreshadowing registry |
| `deduction_logs` | Deduction logs |

#### Timeline

| Table | Purpose |
|-------|---------|
| `timelines` | Timeline definitions |
| `timeline_events` | Timeline events (is_canon distinguishes canon/original) |

#### Canon (fanfic only)

| Table | Purpose |
|-------|---------|
| `canon_sources` | Canon sources |
| `canon_characters` | Canon characters |
| `canon_events` | Canon events |
| `canon_relationships` | Canon relationships |

#### Multi-Project Isolation

All tables have a `project_id` column. Unique constraints are typically `(project_id, name)` or `(project_id, chapter_number)`.

---

### 5. MCP Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/mcp` | JSON-RPC requests (tools/list, tools/call) |
| `GET` | `/health` | Health check |

Configured in `application.yml`:
```yaml
spring.ai.mcp.server:
  protocol: STATELESS   # No session, no lost connections on restart
  type: SYNC
```

---

### 6. 30 MCP Tools

| Category | Tool | Service |
|----------|------|---------|
| **Project Mgmt** | `project_init` | ProjectService |
| | `project_archive` | ProjectService |
| | `project_delete` | ProjectService |
| | `service_reset` | ProjectService |
| | `project_export` | ProjectService |
| | `project_import` | ProjectService |
| **Chapter** | `chapter_sync` | ChapterService |
| | `chapter_get` | ChapterService |
| | `chapter_list` | ChapterService |
| **Character** | `character_save` | CharacterService |
| | `character_status` | CharacterService |
| | `character_snapshot` | CharacterService |
| **Location** | `location_register` | LocationService |
| | `location_update` | LocationService |
| | `location_status` | LocationService |
| **Search** | `rag_search` | RAGService |
| | `semantic_search` | RAGService |
| | `fuzzy_search` | RAGService |
| **Graph** | `graph_query` | GraphService |
| | `graph_path` | GraphService |
| **Timeline** | `timeline_create` | TimelineService |
| | `timeline_event_add` | TimelineService |
| | `timeline_check` | TimelineService |
| **Deduction** | `deduce_behavior` | DeductionService |
| | `deduce_outline` | DeductionService |
| | `deduce_verify` | DeductionService |
| | `register_foreshadowing` | DeductionService |
| **Canon** | `canon_import` | CanonService |
| | `canon_search` | CanonService |
| | `canon_verify` | CanonService |

**Note**: The `review` tool is deprecated. Review is handled by the client AI agent.

---

### 7. How to Add a New Tool

1. Add a method to the corresponding Service class, annotated with `@McpTool` and `@McpToolParam`
2. Use a `record` as the return type (framework auto-serializes)
3. Restart Gateway — it takes effect immediately. No registration code needed

---

### 8. External Services

| Service | Integration |
|---------|-------------|
| **Meilisearch** | Direct HTTP (WebClient), index name `novel_chapters` |
| **LanguageTool** | HTTP POST `/v2/check` (form-encoded) |
| **Neo4j** | `Neo4jClient` executes Cypher directly, no @Node entities |
| **Ollama (bge-m3)** | Called from client side; Gateway only stores vectors in pgvector |

---

### 9. pgvector Notes

- Vector columns are declared as `String`, mapped via `@Type(PgVectorType.class)`
- Vector search `<->` operator requires `nativeQuery=true`
- Vector format: `[0.123, 0.456, ...]` as a string
