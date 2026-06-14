# Novel Weaver Gateway — 开发文档 / 開発ドキュメント / Development Guide

> **CN** 面向开发者。读完就能直接写代码。  
> **JP** 開発者向け。読めばすぐにコードを書ける。  
> **EN** For developers. Read and write code immediately.

---

## 中文

### 1. 是什么

一个 Spring Boot 4.x 应用。做了两件事：

1. **MCP Streamable HTTP 端点** — `POST /mcp`，给 AI 客户端调用
2. **所有数据层查询/写入的编排** — PG + pgvector + ArcadeDB + Meilisearch + LanguageTool

Gateway 不调任何外部 LLM。推演和写作在客户端侧完成。

---

### 2. 项目结构（全部 60 个源文件）

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
│   ├── Item.java                        ← items
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
│   ├── CanonService.java              ← 7 tools
│   ├── TimelineService.java           ← 6 tools
│   ├── ItemService.java                 ← 3 tools
│   ├── UniverseService.java           ← 3 tools
│   ├── GrammarService.java            ← 1 tool
│   └── ItemService.java              ← 3 tools
│   ├── UniverseService.java        ← 3 tools
│   ├── GrammarService.java         ← 1 tool
│   └── (ReviewService 已废弃)         ← 审查由客户端 AI 完成
└── type/
    └── PgVectorType.java                ← Hibernate UserType: String ↔ pgvector

gateway/src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__schema.sql                   ← 初始 16 张表
    ├── V1__schema.sql（统一初始迁移）       ← 补充表
    └── (已合并到 V1)            ← locations 表
```

**没有独立的 controller 层。** MCP 端点由 Spring AI 自动提供，工具通过 `@McpTool` 注解自动注册。

---

### 3. 技术栈

| 层            | 选型                                 |
|--------------|------------------------------------|
| Framework    | Spring Boot 4.0.6 · Java 21        |
| MCP          | STATELESS（无 session）               |
| DB Migration | Flyway（`ddl-auto: validate`）       |
| DB           | PostgreSQL 16+ · pgvector          |
| Graph        | ArcadeDB 5 Community（直接 Cypher）    |
| Search       | Meilisearch（HTTP，无 SDK）            |
| Syntax       | LanguageTool（HTTP POST）            |
| Boilerplate  | Lombok（entity 用 `@Getter @Setter`） |

---

### 4. 数据库（22 张表）

所有 DDL 见 `db/migration/V1__schema.sql`（单文件，所有表）。

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

#### 关系/伏笔

| 表 | 用途 |
|----|------|
| `character_relationships` | 人物关系 |
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


---

### 6. 45 个 MCP 工具

| 分类   | 工具数 | 服务               |
|------|:---:|------------------|
| 项目管理 |  6  | ProjectService   |
| 章节   |  3  | ChapterService   |
| 人物   |  4  | CharacterService |
| 物品   |  3  | ItemService      |
| 地点   |  3  | LocationService  |
| 宇宙   |  3  | UniverseService  |
| 搜索   |  3  | RAGService       |
| 图谱   |  2  | GraphService     |
| 时间线  |  6  | TimelineService  |
| 正典   |  7  | CanonService     |
| 推演   |  4  | DeductionService |
| 语法检查 |  1  | GrammarService   |

> 详见 `agents/reasonix/mcp-tools.md`

---

### 7. 新增一个工具的步骤

1. 在对应 Service 类中加方法，用 `@McpTool` 和 `@McpToolParam` 注解
2. 返回类型用 `record`（框架自动序列化）
3. 重启 Gateway — 即刻生效。不需要任何注册代码

---

### 8. 外部服务

| 服务                  | 接入方式                                    |
|---------------------|-----------------------------------------|
| **Meilisearch**     | 直接 HTTP（WebClient），索引名 `novel_chapters` |
| **LanguageTool**    | HTTP POST `/v2/check`（form-encoded）     |
| **ArcadeDB**        | `ArcadeDBClient` 直接执行 Cypher，无 @Node 实体 |
| **Ollama (bge-m3)** | 客户端侧调用，Gateway 只存向量到 pgvector           |

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
2. **全データ層のクエリ/書き込みのオーケストレーション** — PG + pgvector + ArcadeDB + Meilisearch + LanguageTool

Gateway は外部 LLM を呼び出さない。推論と執筆はクライアント側で完了。

---

### 2. プロジェクト構成（全 60 ソースファイル）

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
│   ├── Item.java                        ← items
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
│   ├── CanonService.java              ← 7 tools
│   ├── TimelineService.java           ← 6 tools
│   └── ItemService.java              ← 3 tools
│   ├── UniverseService.java        ← 3 tools
│   ├── GrammarService.java         ← 1 tool
│   └── (ReviewService 已废弃)         ← 审查由客户端 AI 完成
└── type/
    └── PgVectorType.java                ← Hibernate UserType: String ↔ pgvector

gateway/src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__schema.sql                   ← 初期 16 テーブル
    ├── V1__schema.sql（统一初始迁移）       ← 補完テーブル
    └── (已合并到 V1)            ← locations テーブル
```

**独立した controller 層はない。** MCP エンドポイントは Spring AI が自動提供し、ツールは `@McpTool` アノテーションで自動登録される。

---

### 3. 技術スタック

| 層            | 選定                                 |
|--------------|------------------------------------|
| Framework    | Spring Boot 4.0.6 · Java 21        |
| MCP          | STATELESS（セッションなし）                 |
| DB Migration | Flyway（`ddl-auto: validate`）       |
| DB           | PostgreSQL 16+ · pgvector          |
| Graph        | ArcadeDB 5 Community（直接 Cypher）    |
| Search       | Meilisearch（HTTP、SDK なし）           |
| Syntax       | LanguageTool（HTTP POST）            |
| Boilerplate  | Lombok（entity は `@Getter @Setter`） |

---

### 4. データベース（22 テーブル）

全 DDL は `db/migration/V1__schema.sql`（单文件，所有表） を参照。

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

#### 関係/伏線

| テーブル | 用途 |
|----------|------|
| `character_relationships` | キャラクター関係 |
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

### 6. 45 MCP ツール

| 分類       | ツール数 | サービス             |
|----------|:----:|------------------|
| プロジェクト管理 |  6   | ProjectService   |
| チャプター    |  3   | ChapterService   |
| キャラクター   |  4   | CharacterService |
| アイテム     |  3   | ItemService      |
| ロケーション   |  3   | LocationService  |
| 宇宙       |  3   | UniverseService  |
| 検索       |  3   | RAGService       |
| グラフ      |  2   | GraphService     |
| タイムライン   |  6   | TimelineService  |
| 正典       |  7   | CanonService     |
| 推論       |  4   | DeductionService |
| 文法チェック   |  1   | GrammarService   |

> 詳細は `agents/reasonix/mcp-tools.md` を参照

---

### 7. 新規ツール追加手順

---

### 7. 新規ツール追加手順

1. 該当 Service クラスに `@McpTool` と `@McpToolParam` アノテーション付きメソッドを追加
2. 戻り値の型は `record` を使用（フレームワークが自動シリアライズ）
3. Gateway を再起動——即座に反映される。登録コードは不要

---

### 8. 外部サービス

| サービス                | 接続方式                                           |
|---------------------|------------------------------------------------|
| **Meilisearch**     | 直接 HTTP（WebClient）、索引名 `novel_chapters`        |
| **LanguageTool**    | HTTP POST `/v2/check`（form-encoded）            |
| **ArcadeDB**        | `ArcadeDBClient` で直接 Cypher 実行、@Node entity なし |
| **Ollama (bge-m3)** | クライアント側で呼び出し、Gateway はベクトルを pgvector に保存のみ     |

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
2. **Orchestrates all data-layer queries/writes** — PG + pgvector + ArcadeDB + Meilisearch + LanguageTool

Gateway does NOT call any external LLM. Deduction and writing happen on the client side.

---

### 2. Project Structure (60 source files total)

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
│   ├── Item.java                        ← items
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
│   ├── CanonService.java              ← 7 tools
│   ├── TimelineService.java           ← 6 tools
│   └── ItemService.java              ← 3 tools
│   ├── UniverseService.java        ← 3 tools
│   ├── GrammarService.java         ← 1 tool
│   └── (ReviewService 已废弃)         ← 审查由客户端 AI 完成
└── type/
    └── PgVectorType.java                ← Hibernate UserType: String ↔ pgvector

gateway/src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__schema.sql                   ← Initial 16 tables
    ├── V1__schema.sql（统一初始迁移）       ← Supplementary tables
    └── (已合并到 V1)            ← locations table
```

**There is no separate controller layer.** MCP endpoints are auto-provided by Spring AI, and tools are auto-registered via the `@McpTool` annotation.

---

### 3. Tech Stack

| Layer        | Choice                                 |
|--------------|----------------------------------------|
| Framework    | Spring Boot 4.0.6 · Java 21            |
| MCP          | STATELESS (no sessions)                |
| DB Migration | Flyway (`ddl-auto: validate`)          |
| DB           | PostgreSQL 16+ · pgvector              |
| Graph        | ArcadeDB 5 Community (direct Cypher)   |
| Search       | Meilisearch (HTTP, no SDK)             |
| Syntax       | LanguageTool (HTTP POST)               |
| Boilerplate  | Lombok (`@Getter @Setter` on entities) |

---

### 4. Database (22 tables)

All DDL in `db/migration/V1__schema.sql`（单文件，所有表）.

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

#### Relationships / Foreshadowing

| Table | Purpose |
|-------|---------|
| `character_relationships` | Character relationships |
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

### 6. 45 MCP Tools

| Category     | Count | Service          |
|--------------|:-----:|------------------|
| Project Mgmt |   6   | ProjectService   |
| Chapter      |   3   | ChapterService   |
| Character    |   4   | CharacterService |
| Item         |   3   | ItemService      |
| Location     |   3   | LocationService  |
| Universe     |   3   | UniverseService  |
| Search       |   3   | RAGService       |
| Graph        |   2   | GraphService     |
| Timeline     |   6   | TimelineService  |
| Canon        |   7   | CanonService     |
| Deduction    |   4   | DeductionService |
| Grammar      |   1   | GrammarService   |

> See `agents/reasonix/mcp-tools.md` for full parameter reference

---

### 7. How to Add a New Tool

---

### 7. How to Add a New Tool

1. Add a method to the corresponding Service class, annotated with `@McpTool` and `@McpToolParam`
2. Use a `record` as the return type (framework auto-serializes)
3. Restart Gateway — it takes effect immediately. No registration code needed

---

### 8. External Services

| Service             | Integration                                                      |
|---------------------|------------------------------------------------------------------|
| **Meilisearch**     | Direct HTTP (WebClient), index name `novel_chapters`             |
| **LanguageTool**    | HTTP POST `/v2/check` (form-encoded)                             |
| **ArcadeDB**        | `ArcadeDBClient` executes Cypher directly, no @Node entities     |
| **Ollama (bge-m3)** | Called from client side; Gateway only stores vectors in pgvector |

---

### 9. pgvector Notes

- Vector columns are declared as `String`, mapped via `@Type(PgVectorType.class)`
- Vector search `<->` operator requires `nativeQuery=true`
- Vector format: `[0.123, 0.456, ...]` as a string
