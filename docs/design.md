# Novel Weaver — 小说编织者 / 小説織り手 / Novel Weaver

> **CN** 多项目小说创作辅助系统 · CPU 友好 · RAG 驱动 · 推演引擎 · 版本 v0.3 — 全部决策已定案 · 2026-06-09  
> **JP** マルチプロジェクト小説創作補助システム · CPU フレンドリー · RAG 駆動 · 推論エンジン · バージョン v0.3 — 全決定確定済 · 2026-06-09  
> **EN** Multi-project novel writing assistant · CPU-friendly · RAG-driven · Deduction engine · Version v0.3 — All decisions finalized · 2026-06-09

---

## 一、系统定位 / システムの位置づけ / System Positioning

**CN**  
Novel Weaver 是一个面向小说创作者的辅助系统——不是一个"自动写小说"的工具。

核心哲学：系统提供记忆和检索——人做创作决定。

做什么：
- 记住你写过的一切（章节、人物状态、关系变化）——并能用自然语言查询
- 检索正典设定（同人小说）——谁在什么时候做了什么
- 为推演引擎提供上下文——让 LLM 基于完整前文做推演
- 检查时间线矛盾和人物行为一致性
- 语法和风格检校

不做什么：
- 不替作者做心理推演
- 不自动生成完整章节
- 不评判故事质量
- 不收费——个人心愿项目

**JP**  
Novel Weaver は小説創作のための補助システム——「自動で小説を書く」ツールではありません。

核となる哲学：システムは記憶と検索を提供——創作の決定は人が行う。

行うこと：
- 書いたすべてを記憶（チャプター、キャラクター状態、関係の変化）——自然言語で検索可能
- 正典設定の検索（二次創作）——誰がいつ何をしたか
- 推論エンジンへのコンテキスト提供——LLM が完全な前文に基づいて推論できるように
- タイムラインの矛盾とキャラクター行動の一貫性チェック
- 文法とスタイルの校正

行わないこと：
- 作者に代わって心理推論はしない
- 完全なチャプターを自動生成しない
- ストーリーの品質を判断しない
- 有料化しない——個人の趣味プロジェクト

**EN**  
Novel Weaver is an assistant system for fiction writers — NOT an "auto-write" tool.

Core philosophy: The system provides memory and retrieval — humans make creative decisions.

What it does:
- Remembers everything you've written (chapters, character states, relationship changes) — queryable in natural language
- Retrieves canon settings (fanfic) — who did what, when
- Provides context for the deduction engine — so LLM can deduce based on complete prior text
- Checks timeline consistency and character behavior coherence
- Grammar and style checking

What it doesn't do:
- Does NOT perform psychological deduction for the author
- Does NOT auto-generate complete chapters
- Does NOT judge story quality
- Does NOT charge money — personal passion project

---

## 二、核心约束速查（已定案）/ 核心制約一覧（確定済み）/ Core Constraints (Finalized)

**CN**

| 约束 | 决定 | 说明 |
|------|------|------|
| **硬件** | 纯 CPU，无 GPU | 用户实际情况 |
| **部署** | Docker Compose，全 HTTP 通信 | 所有服务容器化，MCP 走 HTTP |
| **数据规模** | 百万字级，多元宇宙 | 总字数可能上百万。分段索引，单章独立文档 |
| **Ollama 用途** | 必装：bge-m3（embedding） | 可选：qwen2.5:3b（轻量摘要/RAG 合成）——效果不好可直接去掉。写作机 localhost，走 CPU |
| **推演引擎** | Reasonix (Claude)，在写作帧内完成 | 不下放到 Gateway。不调用外部 LLM |
| **数据主权** | **本地 Markdown 是 primary source** | 服务器出问题数据不丢 |
| **同步方向** | 本地 → 服务器（单向同步） | 主路径：Reasonix 写完直接调 API。备用：file-watcher 检测 Obsidian 变化后同步 |
| **编辑器** | Reasonix（主） + Obsidian（辅） | Reasonix 原生调 MCP；Obsidian 靠 file-watcher 同步 |
| **全文搜索** | **Meilisearch** | 模糊搜索（typo tolerance）、前缀匹配、近实时索引。不可用时降级 PG ILIKE |
| **向量搜索** | pgvector + bge-m3 | 语义相似场景检索 |
| **图谱** | Neo4j 5 Community | 人物关系、时间线、事件关联 |
| **语法检查** | LanguageTool (silviof/languagetool) | 中文错别字、重复词、标点检查——Phase 0 内置 |
| **项目隔离** | `project_id` 标签隔离 | Neo4j Community 单库 |
| **项目上限** | 最多 10 个 | 标签隔离绰绰有余 |
| **小说类型** | 原创 + 同人 | 同人需正典采集管线 |
| **版权** | 不涉及——个人心愿创作，不收费 | 正典只存事实性信息 |
| **时间线** | 支持回忆线、穿越、时间闭环 | Neo4j 图建模 |

**JP**

| 制約 | 決定 | 説明 |
|------|------|------|
| **ハードウェア** | 純 CPU、GPU なし | ユーザーの実環境 |
| **デプロイ** | Docker Compose、全 HTTP 通信 | 全サービスコンテナ化、MCP は HTTP |
| **データ規模** | 百万字レベル、マルチユニバース | 総文字数は百万超の可能性。分割索引、単章独立ドキュメント |
| **Ollama 用途** | 必須：bge-m3（embedding） | オプション：qwen2.5:3b（軽量要約/RAG 合成）——効果がなければ削除可。ライティングマシン localhost、CPU |
| **推論エンジン** | Reasonix (Claude)、執筆フレーム内で完了 | Gateway に下ろさない。外部 LLM を呼ばない |
| **データ主権** | **ローカル Markdown が primary source** | サーバー障害でもデータ消失なし |
| **同期方向** | ローカル → サーバー（単方向同期） | 主経路：Reasonix が書き終えたら直接 API 呼び出し。予備：file-watcher が Obsidian の変更を検出して同期 |
| **エディター** | Reasonix（主） + Obsidian（補） | Reasonix はネイティブに MCP 呼び出し。Obsidian は file-watcher 経由 |
| **全文検索** | **Meilisearch** | あいまい検索（typo tolerance）、前方一致、ニアリアルタイム索引。利用不可時は PG ILIKE にフォールバック |
| **ベクトル検索** | pgvector + bge-m3 | 意味的類似シーンの検索 |
| **グラフ** | Neo4j 5 Community | キャラクター関係、タイムライン、イベント関連 |
| **文法チェック** | LanguageTool (silviof/languagetool) | 中国語の誤字、重複語、句読点チェック——Phase 0 から内蔵 |
| **プロジェクト分離** | `project_id` タグ分離 | Neo4j Community 単一データベース |
| **プロジェクト上限** | 最大 10 | タグ分離で十分 |
| **小説タイプ** | オリジナル＋二次創作 | 二次創作は正典収集パイプラインが必要 |
| **著作権** | 関与しない——個人の趣味創作、無料 | 正典は事実情報のみ保存 |
| **タイムライン** | 回想、タイムトラベル、時間ループ対応 | Neo4j グラフモデリング |

**EN**

| Constraint | Decision | Notes |
|------------|----------|-------|
| **Hardware** | CPU only, no GPU | User's actual environment |
| **Deployment** | Docker Compose, all HTTP | All services containerized, MCP over HTTP |
| **Data Scale** | Million-char level, multi-verse | Total characters may exceed one million. Segmented indexing, chapters as independent docs |
| **Ollama usage** | Required: bge-m3 (embedding) | Optional: qwen2.5:3b (lightweight summary/RAG synthesis) — remove if ineffective. On writing machine localhost, CPU |
| **Deduction engine** | Reasonix (Claude), inside writing frame | Not offloaded to Gateway. No external LLM calls |
| **Data sovereignty** | **Local Markdown is primary source** | Server outage doesn't lose data |
| **Sync direction** | Local → Server (one-way) | Primary: Reasonix calls API directly after writing. Backup: file-watcher detects Obsidian changes |
| **Editor** | Reasonix (primary) + Obsidian (auxiliary) | Reasonix calls MCP natively; Obsidian syncs via file-watcher |
| **Full-text search** | **Meilisearch** | Fuzzy search (typo tolerance), prefix match, near-real-time indexing. Falls back to PG ILIKE when unavailable |
| **Vector search** | pgvector + bge-m3 | Semantic similarity scene retrieval |
| **Graph** | Neo4j 5 Community | Character relationships, timelines, event connections |
| **Grammar check** | LanguageTool (silviof/languagetool) | Chinese typo, duplicate word, punctuation check — built-in from Phase 0 |
| **Project isolation** | `project_id` tag isolation | Neo4j Community single database |
| **Project limit** | Max 10 | Tag isolation is more than sufficient |
| **Fiction type** | Original + Fanfic | Fanfic requires canon collection pipeline |
| **Copyright** | Not involved — personal passion, free | Canon stores factual info only |
| **Timeline** | Supports flashback, time travel, time loops | Neo4j graph modeling |

---

## 三、整体架构 / 全体アーキテクチャ / Overall Architecture

**CN**

```
┌── 写作机（你的电脑）──────────────────────────────┐
│                                                    │
│  Reasonix (Claude)                                 │
│  ├── 推演角色行为、章节大纲——自己推理                 │
│  ├── 写正文——按写作指纹                             │
│  ├── 调 Ollama (localhost:11434)                   │
│  │   ├── bge-m3 → embedding（必装）                │
│  │   └── qwen2.5:3b → 摘要/关键词（可选，效果不佳可直接去掉）│
│  └── 调 Gateway (HTTP)                             │
│      ├── 查数据 → PG/Neo4j/Meilisearch              │
│      ├── chapter_sync → 同步存储                    │
│      └── review → 审查                             │
│                                                    │
│  本地 .md 文件                                      │
│  ~/novel-vault/                                    │
│  ├── project-A/{chapters/, outlines/, ...}          │
│  └── ...                                           │
│                                                    │
└────────────────────┬───────────────────────────────┘
                     │ HTTP (服务器 IP:8080)
                     │
┌────────────────────▼───────────────────────────────┐
│                 服务器 Docker Network                │
│                                                    │
│  ┌── Spring Boot Gateway ──────────────┐           │
│  │  port: 8080                          │           │
│  │  /mcp → MCP Streamable HTTP          │           │
│  │        (Spring AI @McpTool 自动注册)  │           │
│  │                                       │           │
│  │  Service 层（纯数据，无 LLM）          │           │
│  │  ├── ProjectService    (6 tools)     │           │
│  │  ├── ChapterService    (3 tools)     │           │
│  │  ├── CharacterService  (4 tools)     │           │
│  │  ├── RAGService        (3 tools)     │           │
│  │  ├── GraphService      (2 tools)     │           │
│  │  ├── DeductionService  (4 tools)     │           │
│  │  ├── CanonService      (7 tools)     │           │
│  │  ├── TimelineService   (6 tools)     │           │
│  │  ├── ItemService       (3 tools)
│  │  ├── UniverseService   (3 tools)
│  │  ├── GrammarService    (1 tool)
│  │  ├── LocationService   (3 tools)     │           │
│  └──────────────────────────────────────┘           │
│                                                    │
│  ┌───────┐ ┌───────┐ ┌────────────┐ ┌──────────┐  │
│  │PG 17  │ │Neo4j 5│ │Meilisearch │ │LangTool  │  │
│  │+vector│ │标签隔 │ │ 模糊搜索    │ │ 语法检查  │  │
│  │5432   │ │7687   │ │7700        │ │8010      │  │
│  └───────┘ └───────┘ └────────────┘ └──────────┘  │
└────────────────────────────────────────────────────┘
```

**JP**

```
┌── ライティングマシン（あなたのPC）──────────────────┐
│                                                    │
│  Reasonix (Claude)                                 │
│  ├── キャラクター行動、章概要の推論——自分で推論      │
│  ├── 本文を書く——執筆指針に従って                    │
│  ├── Ollama を呼ぶ (localhost:11434)               │
│  │   ├── bge-m3 → embedding（必須）                │
│  │   └── qwen2.5:3b → 要約/キーワード（オプション、効果なければ削除）│
│  └── Gateway を呼ぶ (HTTP)                         │
│      ├── データ照会 → PG/Neo4j/Meilisearch          │
│      ├── chapter_sync → 同期保存                    │
│      └── review → レビュー                         │
│                                                    │
│  ローカル .md ファイル                               │
│  ~/novel-vault/                                    │
│  ├── project-A/{chapters/, outlines/, ...}          │
│  └── ...                                           │
│                                                    │
└────────────────────┬───────────────────────────────┘
                     │ HTTP (サーバー IP:8080)
                     │
┌────────────────────▼───────────────────────────────┐
│                 サーバー Docker Network              │
│                                                    │
│  ┌── Spring Boot Gateway ──────────────┐           │
│  │  port: 8080                          │           │
│  │  /mcp → MCP Streamable HTTP          │           │
│  │        (Spring AI @McpTool 自動登録)  │           │
│  │                                       │           │
│  │  Service 層（純データ、LLM なし）      │           │
│  │  ├── ProjectService    (6 tools)     │           │
│  │  ├── ChapterService    (3 tools)     │           │
│  │  ├── CharacterService  (4 tools)     │           │
│  │  ├── RAGService        (3 tools)     │           │
│  │  ├── GraphService      (2 tools)     │           │
│  │  ├── DeductionService  (4 tools)     │           │
│  │  ├── CanonService      (7 tools)     │           │
│  │  ├── TimelineService   (6 tools)     │           │
│  │  ├── ItemService       (3 tools)
│  │  ├── UniverseService   (3 tools)
│  │  ├── GrammarService    (1 tool)
│  │  ├── LocationService   (3 tools)     │           │
│  └──────────────────────────────────────┘           │
│                                                    │
│  ┌───────┐ ┌───────┐ ┌────────────┐ ┌──────────┐  │
│  │PG 17  │ │Neo4j 5│ │Meilisearch │ │LangTool  │  │
│  │+vector│ │タグ分 │ │ あいまい検索│ │ 文法チェック│
│  │5432   │ │7687   │ │7700        │ │8010      │  │
│  └───────┘ └───────┘ └────────────┘ └──────────┘  │
└────────────────────────────────────────────────────┘
```

**EN**

```
┌── Writing Machine (Your PC) ───────────────────────┐
│                                                    │
│  Reasonix (Claude)                                 │
│  ├── Deduces character behavior, chapter outlines  │
│  ├── Writes prose — follows writing fingerprint    │
│  ├── Calls Ollama (localhost:11434)                │
│  │   ├── bge-m3 → embedding (required)             │
│  │   └── qwen2.5:3b → summary/keywords (optional) │
│  └── Calls Gateway (HTTP)                          │
│      ├── Query data → PG/Neo4j/Meilisearch          │
│      ├── chapter_sync → sync storage               │
│      └── review → review                           │
│                                                    │
│  Local .md files                                   │
│  ~/novel-vault/                                    │
│  ├── project-A/{chapters/, outlines/, ...}          │
│  └── ...                                           │
│                                                    │
└────────────────────┬───────────────────────────────┘
                     │ HTTP (Server IP:8080)
                     │
┌────────────────────▼───────────────────────────────┐
│                 Server Docker Network                │
│                                                    │
│  ┌── Spring Boot Gateway ──────────────┐           │
│  │  port: 8080                          │           │
│  │  /mcp → MCP Streamable HTTP          │           │
│  │        (Spring AI @McpTool auto-reg) │           │
│  │                                       │           │
│  │  Service layer (pure data, no LLM)   │           │
│  │  ├── ProjectService    (6 tools)     │           │
│  │  ├── ChapterService    (3 tools)     │           │
│  │  ├── CharacterService  (4 tools)     │           │
│  │  ├── RAGService        (3 tools)     │           │
│  │  ├── GraphService      (2 tools)     │           │
│  │  ├── DeductionService  (4 tools)     │           │
│  │  ├── CanonService      (7 tools)     │           │
│  │  ├── TimelineService   (6 tools)     │           │
│  │  ├── ItemService       (3 tools)
│  │  ├── UniverseService   (3 tools)
│  │  ├── GrammarService    (1 tool)
│  │  ├── LocationService   (3 tools)     │           │
│  └──────────────────────────────────────┘           │
│                                                    │
│  ┌───────┐ ┌───────┐ ┌────────────┐ ┌──────────┐  │
│  │PG 17  │ │Neo4j 5│ │Meilisearch │ │LangTool  │  │
│  │+vector│ │tag    │ │fuzzy search│ │grammar   │  │
│  │5432   │ │7687   │ │7700        │ │8010      │  │
│  └───────┘ └───────┘ └────────────┘ └──────────┘  │
└────────────────────────────────────────────────────┘
```

### 组件职责表 / コンポーネント責務表 / Component Responsibilities

**CN**

| 组件 | 位置 | 职责 |
|------|------|------|
| **Reasonix (Claude)** | 写作机 | 🔴 全部创作层——推演、写作、审校。调写作机上的 Ollama 做 embedding/摘要。调服务器上的 Gateway 做数据查询/同步 |
| **Ollama (bge-m3)** | 写作机 localhost:11434 | embedding 向量化（必装）。偶尔用于轻量摘要（可选 qwen2.5:3b）。不走服务器——写作机上本地调用 |
| **Gateway** | 服务器 Docker | 纯数据层——无 LLM 依赖。处理数据 CRUD、搜索转发、图谱查询、语法检查 |
| **PostgreSQL + pgvector** | 服务器 Docker | 结构化存储 + 向量索引 |
| **Neo4j** | 服务器 Docker | 人物关系图谱、时间线 |
| **Meilisearch** | 服务器 Docker | 全文搜索 + 模糊搜索 |
| **LanguageTool** | 服务器 Docker | 中文语法/错别字/风格检查 |

**JP**

| コンポーネント | 配置 | 責務 |
|--------------|------|------|
| **Reasonix (Claude)** | ライティングマシン | 🔴 全創作層——推論、執筆、校正。ライティングマシンの Ollama で embedding/要約。サーバーの Gateway でデータ照会/同期 |
| **Ollama (bge-m3)** | ライティングマシン localhost:11434 | embedding ベクトル化（必須）。軽量要約に使うことも（オプション qwen2.5:3b）。サーバーは経由せず、ローカルで呼び出し |
| **Gateway** | サーバー Docker | 純データ層——LLM 依存なし。データ CRUD、検索転送、グラフ照会、文法チェック |
| **PostgreSQL + pgvector** | サーバー Docker | 構造化ストレージ＋ベクトル索引 |
| **Neo4j** | サーバー Docker | キャラクター関係グラフ、タイムライン |
| **Meilisearch** | サーバー Docker | 全文検索＋あいまい検索 |
| **LanguageTool** | サーバー Docker | 中国語文法/誤字/スタイルチェック |

**EN**

| Component | Location | Responsibilities |
|-----------|----------|-----------------|
| **Reasonix (Claude)** | Writing machine | 🔴 All creative layers — deduction, writing, review. Calls local Ollama for embedding/summary. Calls server Gateway for data query/sync |
| **Ollama (bge-m3)** | Writing machine localhost:11434 | Embedding vectorization (required). Occasional light summarization (optional qwen2.5:3b). Local call, not via server |
| **Gateway** | Server Docker | Pure data layer — no LLM dependency. Handles CRUD, search forwarding, graph queries, grammar check |
| **PostgreSQL + pgvector** | Server Docker | Structured storage + vector index |
| **Neo4j** | Server Docker | Character relationship graph, timelines |
| **Meilisearch** | Server Docker | Full-text search + fuzzy search |
| **LanguageTool** | Server Docker | Chinese grammar/typo/style check |

### 为什么 Meilisearch 不能省 / Meilisearch が必須の理由 / Why Meilisearch Is Not Optional

**CN**
- **模糊搜索（Typo Tolerance）**：作者打错角色名（"凯尼"→"凯妮"）或记不全（"月之"→"月之泪"）仍能匹配。PG 全文搜索做不到这个
- **近实时索引**：章节同步后秒级可搜
- **百万字规模**：单章 1-2 万字作为独立文档，Meilisearch 默认单文档限制 5MB——绰绰有余。100 章也才 100-200 个文档，索引体积极小

Meilisearch 为主，PG ILIKE 为后备：如果 Meilisearch 容器挂了，`fuzzy_search` 工具自动降级到 `content ILIKE '%keyword%'`——丢模糊搜索但不会完全不可用。正常运行时走 Meilisearch。

**JP**
- **あいまい検索（Typo Tolerance）**：作者がキャラクター名を打ち間違えても（"凯尼"→"凯妮"）またはうろ覚えでも（"月之"→"月之泪"）マッチする。PG 全文検索では不可能
- **ニアリアルタイム索引**：チャプター同期後、秒単位で検索可能
- **百万字規模**：1章1-2万字を独立ドキュメントとして、Meilisearch のデフォルト単一ドキュメント制限 5MB——十分すぎる。100章でも100-200ドキュメント、索引サイズはごく小さい

Meilisearch を主とし、PG ILIKE を予備とする：Meilisearch コンテナが落ちた場合、`fuzzy_search` ツールは自動的に `content ILIKE '%keyword%'` にフォールバック——あいまい検索は失うが完全には使えなくならない。通常時は Meilisearch。

**EN**
- **Fuzzy Search (Typo Tolerance)**: Even if the author misspells a character name ("Kainé"→"Kaine") or can't remember the full name, it still matches. PG full-text search can't do this
- **Near-real-time indexing**: Searchable seconds after chapter sync
- **Million-character scale**: 10-20k characters per chapter as independent documents. Meilisearch default doc limit is 5MB — more than enough. 100 chapters = only 100-200 documents, tiny index size

Meilisearch primary, PG ILIKE fallback: If the Meilisearch container goes down, `fuzzy_search` auto-degrades to `content ILIKE '%keyword%'` — loses fuzzy search but stays functional. Normal operation uses Meilisearch.

### 关键设计决策 / 重要な設計判断 / Key Design Decisions

**CN**

**1. 两层架构：创作层在写作机，数据层在服务器**

Reasonix（Claude）在写作机上统管全部 AI 工作——推演、写作、正文生成都在同一个上下文里完成，不下放到服务器。Gateway 不调用任何外部 LLM，只做数据查询、存储转发、语法检查。

```
Reasonix (写作机)
  ├── 调 Ollama (localhost:11434) → embedding/摘要/关键词
  ├── 调 Gateway (HTTP)           → 数据查询/同步
  └── 自己推演 + 写作

Gateway (服务器 Docker)
  └── 纯数据层——无 LLM 依赖
```

**JP**

**1. 二層アーキテクチャ：創作層はライティングマシン、データ層はサーバー**

Reasonix（Claude）がライティングマシン上で全 AI 作業を統括——推論、執筆、本文生成をすべて同一コンテキスト内で完結させ、サーバーには下ろさない。Gateway は外部 LLM を一切呼ばず、データ照会、保存転送、文法チェックのみ。

```
Reasonix (ライティングマシン)
  ├── Ollama (localhost:11434) → embedding/要約/キーワード
  ├── Gateway (HTTP)           → データ照会/同期
  └── 自分で推論＋執筆

Gateway (サーバー Docker)
  └── 純データ層——LLM 依存なし
```

**EN**

**1. Two-layer architecture: Creative layer on writing machine, data layer on server**

Reasonix (Claude) manages all AI work on the writing machine — deduction, writing, and generation all happen in the same context, not on the server. Gateway does not call any external LLM; it only does data query, storage forwarding, and grammar check.

```
Reasonix (Writing Machine)
  ├── Calls Ollama (localhost:11434) → embedding/summary/keywords
  ├── Calls Gateway (HTTP)           → data query/sync
  └── Self-deduction + writing

Gateway (Server Docker)
  └── Pure data layer — no LLM dependency
```

---

**CN**

**2. 本地文件是 primary source，Reasonix 是主写作界面**

数据库是**索引层**——为搜索和 RAG 服务。本地 `.md` 文件是数据的最终保障——服务器挂了数据不丢。

但数据流的**主要入口不是 file-watcher，是 Reasonix**：

```
Reasonix (写作 agent)
  → 生成章节 → 写到本地 .md 文件
  → 同时直接调 MCP chapter_sync → DB 同步
  → 语法检查 → 修正 → 再同步
```

file-watcher 是**备用通道**——当你用 Obsidian 手动编辑了文件，它检测变化并同步到 DB。但正常写作流程里，Reasonix 直接调 MCP，不走 file-watcher 中转。

**JP**

**2. ローカルファイルが primary source、Reasonix が主執筆インターフェース**

データベースは**索引層**——検索と RAG のため。ローカル `.md` ファイルがデータの最終保証——サーバーが落ちてもデータ消失なし。

ただしデータフローの**主要入口は file-watcher ではなく Reasonix**：

```
Reasonix (執筆エージェント)
  → チャプター生成 → ローカル .md ファイルに書き込み
  → 同時に直接 MCP chapter_sync → DB 同期
  → 文法チェック → 修正 → 再同期
```

file-watcher は**予備チャネル**——Obsidian でファイルを手動編集した場合、変更を検出して DB に同期する。通常の執筆フローでは Reasonix が直接 MCP を呼び、file-watcher を経由しない。

**EN**

**2. Local files are primary source, Reasonix is the main writing interface**

The database is an **index layer** — for search and RAG. Local `.md` files are the ultimate safeguard — server crash won't lose data.

However, the **primary entry point is Reasonix, not file-watcher**:

```
Reasonix (writing agent)
  → Generates chapter → writes to local .md file
  → Directly calls MCP chapter_sync → DB sync
  → Grammar check → fix → re-sync
```

file-watcher is the **backup channel** — when you manually edit files in Obsidian, it detects changes and syncs to DB. But in normal writing flow, Reasonix calls MCP directly, bypassing file-watcher.

---

**CN**

**3. 段落切分：两者结合**

先按 markdown 标题（`##`）切分场景，场景内超过 1000 字按 500-800 字再切，相邻段重叠 100 字。这样既能按场景检索，又能保证向量精度。

**JP**

**3. 段落分割：両者を組み合わせ**

まず Markdown 見出し（`##`）でシーンを分割し、シーン内で 1000 字を超える場合は 500-800 字でさらに分割、隣接段落は 100 字重複させる。これによりシーン単位の検索とベクトル精度の両立を実現。

**EN**

**3. Paragraph segmentation: hybrid approach**

First split by markdown headings (`##`) into scenes, then further split scenes exceeding 1000 characters into 500-800 character chunks with 100-character overlap between adjacent chunks. This enables both scene-level retrieval and vector precision.

---

## 四、写作界面与文件同步 / 執筆インターフェースとファイル同期 / Writing Interface & File Sync

### 4.1 两个入口，一个真相源 / 二つの入口、一つの真実源 / Two Entries, One Source of Truth

**CN**

| 入口 | 定位 | MCP 调用方式 | 同步到 DB 的时机 |
|------|------|------------|----------------|
| **Reasonix（主）** | 日常写作——你通过 Claude 写章节、推演行为、搜索 | Reasonix 在写作过程中**直接调 MCP 工具**（`chapter_sync`, `rag_search`, `deduce_behavior` 等） | Reasonix 写完一段或一章后主动调用 `chapter_sync` |
| **Obsidian（辅）** | 离线浏览、手动修改、手机端临时编辑 | 不直接调 MCP——改完文件后由 file-watcher 检测 → 自动同步 | file-watcher 检测文件变化后调用 `chapter_sync` |

**JP**

| 入口 | 位置づけ | MCP 呼び出し方法 | DB 同期タイミング |
|------|---------|-----------------|------------------|
| **Reasonix（主）** | 日常執筆——Claude でチャプター執筆、行動推論、検索 | Reasonix が執筆中に**直接 MCP ツールを呼ぶ**（`chapter_sync`, `rag_search`, `deduce_behavior` 等） | Reasonix が一段落または一章書き終えた後に能動的に `chapter_sync` を呼ぶ |
| **Obsidian（補）** | オフラインブラウズ、手動編集、モバイル端末での一時編集 | MCP は直接呼ばない——ファイル変更後 file-watcher が検出→自動同期 | file-watcher がファイル変更を検出した後に `chapter_sync` を呼ぶ |

**EN**

| Entry | Role | MCP Call Method | Sync Timing |
|-------|------|----------------|-------------|
| **Reasonix (Primary)** | Daily writing — write chapters, deduct behavior, search via Claude | Reasonix **directly calls MCP tools** during writing (`chapter_sync`, `rag_search`, `deduce_behavior`, etc.) | Proactively calls `chapter_sync` after writing a section or chapter |
| **Obsidian (Auxiliary)** | Offline browsing, manual editing, mobile temporary edits | Does NOT call MCP directly — file-watcher detects changes → auto-sync | file-watcher calls `chapter_sync` after detecting file changes |

**CN 数据流（主路径）**

```
你 → Reasonix (Claude) → 生成/修改章节
                           ├── 写入本地 .md 文件（数据保障）
                           └── 直接调 MCP chapter_sync → DB 同步
                                                        ├── PG + 版本记录
                                                        ├── 分段 + embedding
                                                        ├── Meilisearch 索引
                                                        └── Neo4j 节点
```

**JP データフロー（主経路）**

```
あなた → Reasonix (Claude) → チャプター生成/修正
                               ├── ローカル .md に書き込み（データ保証）
                               └── 直接 MCP chapter_sync → DB 同期
                                                            ├── PG + バージョン記録
                                                            ├── 分割 + embedding
                                                            ├── Meilisearch 索引
                                                            └── Neo4j ノード
```

**EN Data Flow (Primary Path)**

```
You → Reasonix (Claude) → Generate/edit chapter
                           ├── Write to local .md (data guarantee)
                           └── Direct MCP chapter_sync → DB sync
                                                        ├── PG + version history
                                                        ├── Segmentation + embedding
                                                        ├── Meilisearch index
                                                        └── Neo4j nodes
```

**CN 数据流（备用路径）**：`Obsidian 编辑 → Ctrl+S → file-watcher 检测 → POST /api/sync/chapter → DB 同步`

**JP データフロー（予備経路）**：`Obsidian 編集 → Ctrl+S → file-watcher 検出 → POST /api/sync/chapter → DB 同期`

**EN Data Flow (Backup Path)**: `Obsidian edit → Ctrl+S → file-watcher detects → POST /api/sync/chapter → DB sync`

### 4.2 目录结构约定 / ディレクトリ構成約定 / Directory Structure Convention

```
~/novel-vault/
├── project-A/
│   ├── meta.yaml
│   ├── characters.yaml
│   ├── timeline.yaml
│   ├── outlines/
│   │   ├── 第0001章-降临.md
│   │   └── ...
│   └── chapters/
│       ├── 第0001章-降临.md
│       ├── 第0002章-xxx.md
│       └── ...
├── project-B/
│   └── ...
└── _templates/
    ├── chapter-template.md
    └── outline-template.md
```

**CN**：详细目录规范见 `directory-spec.md`。

**JP**：詳細なディレクトリ仕様は `directory-spec.md` を参照。

**EN**：See `directory-spec.md` for detailed directory specifications.

### 4.3 Reasonix 写作时如何用 MCP / Reasonix 執筆時の MCP 活用法 / How Reasonix Uses MCP While Writing

**CN**：Reasonix 在写作会话中随时可以调 MCP 工具——不需要切出编辑器。一个写作帧的示意：

```
你: "写第0006章"

Reasonix:
  → rag_search("前五章凯妮的心理变化")
  → 读 outlines/第0006章-xxx.md
  → deduce_behavior(场景="集装箱内放歌", chars=["凯妮","Tyrann"])
  → 按写作指纹生成正文 → 写到 正文/第0006章-xxx.md
  → chapter_sync(内容)
  → character_snapshot(凯妮, chapter=6)
  → register_foreshadowing(F006)

你: "好，继续第0007章"
```

Reasonix 调 MCP 和写本地文件是**同一帧内的连续操作**——写完立刻就同步，不需要等 file-watcher。

**JP**：Reasonix は執筆セッション中いつでも MCP ツールを呼べる——エディターを切り替える必要はない。

```
あなた: "第0006章を書いて"

Reasonix:
  → rag_search("過去五章のキャリーの心理変化")
  → outlines/第0006章-xxx.md を読む
  → deduce_behavior(シーン="コンテナ内で歌う", chars=["凯妮","Tyrann"])
  → 執筆指針に従い本文生成 → 正文/第0006章-xxx.md に書き込み
  → chapter_sync(内容)
  → character_snapshot(凯妮, chapter=6)
  → register_foreshadowing(F006)

あなた: "よし、第0007章を続けて"
```

Reasonix の MCP 呼び出しとローカルファイル書き込みは**同一フレーム内の連続操作**——書き終えたら即座に同期、file-watcher を待つ必要なし。

**EN**：Reasonix can call MCP tools anytime during a writing session — no need to switch editors.

```
You: "Write chapter 0006"

Reasonix:
  → rag_search("Kainé's psychological changes in first five chapters")
  → Read outlines/ch0006-xxx.md
  → deduce_behavior(scene="singing in container", chars=["Kainé","Tyrann"])
  → Generate prose per fingerprint → write to chapters/ch0006-xxx.md
  → chapter_sync(content)
  → character_snapshot(Kainé, chapter=6)
  → register_foreshadowing(F006)

You: "Good, continue with chapter 0007"
```

Reasonix calling MCP and writing local files are **consecutive operations in the same frame** — synced immediately, no need to wait for file-watcher.

### 4.4 Obsidian 定位 / Obsidian の位置づけ / Obsidian's Role

**CN**：Obsidian 降级为备用查看器/离线编辑器。不想开 Reasonix 时翻看前面章节，或手机/平板上改一段话。但 MCP 工具集成等复杂操作不在 Obsidian 中完成——Reasonix 去做。

**JP**：Obsidian は予備ビューアー/オフラインエディターに格下げ。Reasonix を開かずに過去のチャプターを読んだり、スマホ/タブレットで一部修正したりする用途。MCP ツール連携などの複雑な操作は Obsidian では行わず、Reasonix で行う。

**EN**：Obsidian is downgraded to a backup viewer/offline editor. For browsing previous chapters without opening Reasonix, or quick edits on phone/tablet. Complex operations like MCP tool integration are not done in Obsidian — Reasonix handles those.

---

## 五、Docker 部署 / Docker デプロイ / Docker Deployment

### 5.1 两个 compose 文件 / 二つの compose ファイル / Two Compose Files

**CN**

| 文件 | 内容 | 用途 |
|------|------|------|
| `infra.yml` | PG + Neo4j + Meilisearch + LanguageTool | 基础设施——写一次后基本不改 |
| `docker-compose.yml` | Spring Boot Gateway | 应用——经常改代码，单独重启 |

**JP**

| ファイル | 内容 | 用途 |
|----------|------|------|
| `infra.yml` | PG + Neo4j + Meilisearch + LanguageTool | インフラ——一度書けば基本的に変更なし |
| `docker-compose.yml` | Spring Boot Gateway | アプリケーション——コード変更頻繁、個別再起動 |

**EN**

| File | Contents | Purpose |
|------|----------|---------|
| `infra.yml` | PG + Neo4j + Meilisearch + LanguageTool | Infrastructure — write once, rarely change |
| `docker-compose.yml` | Spring Boot Gateway | Application — frequent code changes, restart independently |

### 5.2 端口映射 / ポートマッピング / Port Mapping

| Service | Internal Port | External Port | Notes |
|---------|--------------|---------------|-------|
| PostgreSQL | 5432 | 5432 | pgvector enabled |
| Neo4j | 7687 (Bolt), 7474 (HTTP) | 7687 | No external HTTP |
| Meilisearch | 7700 | 7700 | HTTP API |
| LanguageTool | 8010 | 8010 | HTTP POST /v2/check |
| Gateway | 8080 | 8080 | MCP endpoint |

**CN**：所有服务绑定 localhost，不走 TLS。内部网络用 Docker network，外部只暴露 Gateway 的 8080 端口。写作机通过 HTTP 调用服务器 IP 的 8080 端口。

**JP**：全サービスは localhost にバインド、TLS は使わない。内部ネットワークは Docker network、外部には Gateway の 8080 ポートのみ公開。ライティングマシンは HTTP でサーバー IP の 8080 ポートを呼ぶ。

**EN**：All services bind to localhost, no TLS. Internal communication via Docker network; only Gateway port 8080 is exposed externally. Writing machine calls server IP:8080 over HTTP.

### 5.3 部署流程 / デプロイ手順 / Deployment Procedure

**CN**

```bash
# 1. 首次部署——启动基础设施
docker compose -f infra.yml up -d

# 2. 构建 Gateway
cd gateway
./mvnw package -DskipTests

# 3. 启动 Gateway
docker compose up -d --build

# 4. 验证
curl http://localhost:8883/health
# → ok
```

**JP**

```bash
# 1. 初回デプロイ——インフラ起動
docker compose -f infra.yml up -d

# 2. Gateway ビルド
cd gateway
./mvnw package -DskipTests

# 3. Gateway 起動
docker compose up -d --build

# 4. 確認
curl http://localhost:8883/health
# → ok
```

**EN**

```bash
# 1. First deploy — start infrastructure
docker compose -f infra.yml up -d

# 2. Build Gateway
cd gateway
./mvnw package -DskipTests

# 3. Start Gateway
docker compose up -d --build

# 4. Verify
curl http://localhost:8883/health
# → ok
```

---

## 六、数据模型 / データモデル / Data Model

### 6.1 ER 概览 / ER 概要 / ER Overview

**CN**

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  projects   │──1:N│    chapters      │──1:N│ chapter_versions  │
└─────────────┘     └──────────────────┘     └───────────────────┘
                          │ 1:N                       │ 1:N
                          ▼                           ▼
                    ┌──────────────────┐     ┌───────────────────┐
                    │ character_       │     │ chapter_          │
                    │ snapshots        │     │ paragraphs        │
                    └──────────────────┘     │ (向量 indexed)    │
                          │                  └───────────────────┘
                          │ 1:N
                          ▼
                    ┌──────────────────┐     ┌───────────────────┐
                    │ character_       │     │ character_        │
                    │ profiles         │──1:N│ voiceprints       │
                    └──────────────────┘     └───────────────────┘
                          │
                          │ 1:N
                          ▼
                    ┌──────────────────┐
                    │ character_       │
                    │ relationships    │
                    └──────────────────┘
```

**JP**

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  projects   │──1:N│    chapters      │──1:N│ chapter_versions  │
└─────────────┘     └──────────────────┘     └───────────────────┘
                          │ 1:N                       │ 1:N
                          ▼                           ▼
                    ┌──────────────────┐     ┌───────────────────┐
                    │ character_       │     │ chapter_          │
                    │ snapshots        │     │ paragraphs        │
                    └──────────────────┘     │ (ベクトル索引)    │
                          │                  └───────────────────┘
                          │ 1:N
                          ▼
                    ┌──────────────────┐     ┌───────────────────┐
                    │ character_       │     │ character_        │
                    │ profiles         │──1:N│ voiceprints       │
                    └──────────────────┘     └───────────────────┘
                          │
                          │ 1:N
                          ▼
                    ┌──────────────────┐
                    │ character_       │
                    │ relationships    │
                    └──────────────────┘
```

**EN**

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  projects   │──1:N│    chapters      │──1:N│ chapter_versions  │
└─────────────┘     └──────────────────┘     └───────────────────┘
                          │ 1:N                       │ 1:N
                          ▼                           ▼
                    ┌──────────────────┐     ┌───────────────────┐
                    │ character_       │     │ chapter_          │
                    │ snapshots        │     │ paragraphs        │
                    └──────────────────┘     │ (vector indexed)  │
                          │                  └───────────────────┘
                          │ 1:N
                          ▼
                    ┌──────────────────┐     ┌───────────────────┐
                    │ character_       │     │ character_        │
                    │ profiles         │──1:N│ voiceprints       │
                    └──────────────────┘     └───────────────────┘
                          │
                          │ 1:N
                          ▼
                    ┌──────────────────┐
                    │ character_       │
                    │ relationships    │
                    └──────────────────┘
```

### 6.2 表结构 / テーブル構造 / Table Structure

**CN**：所有表字段定义见 `schema-v1-flyway.sql`。以下只列出关键说明。

**JP**：全テーブルのフィールド定義は `schema-v1-flyway.sql` を参照。以下は主要な説明のみ。

**EN**：All table field definitions in `schema-v1-flyway.sql`. Only key descriptions listed below.

| 表 / Table                 | CN 用途    | JP 用途          | EN Purpose                   |
|---------------------------|----------|----------------|------------------------------|
| `projects`                | 项目元信息    | プロジェクトメタ情報     | Project metadata             |
| `chapters`                | 章节正文+元数据 | チャプター本文＋メタデータ  | Chapter body + metadata      |
| `chapter_versions`        | 每次保存的版本  | 保存ごとのバージョン     | Version per save             |
| `chapter_paragraphs`      | 段落级向量索引  | 段落レベルベクトル索引    | Paragraph-level vector index |
| `character_profiles`      | 角色画像     | キャラクター設定       | Character profiles           |
| `character_snapshots`     | 每章后状态快照  | 章ごとの状態スナップショット | Per-chapter state snapshot   |
| `character_relationships` | 人物关系     | キャラクター関係       | Character relationships      |
| `character_voiceprints`   | 声纹样本     | 声紋サンプル         | Voiceprint samples           |
| `foreshadowing_index`     | 伏笔登记     | 伏線管理           | Foreshadowing registry       |
| `deduction_logs`          | 推演日志     | 推論ログ           | Deduction logs               |
| `locations`               | 地点档案     | ロケーション管理       | Location registry            |
| `items`                   | 物品档案     | アイテムアーカイブ      | Item registry                |
| `timelines`               | 时间线定义    | タイムライン定義       | Timeline definitions         |
| `timeline_events`         | 时间线事件    | タイムラインイベント     | Timeline events              |
| `canon_sources`           | 正典来源     | 正典ソース          | Canon sources                |
| `canon_characters`        | 正典人物     | 正典キャラクター       | Canon characters             |
| `canon_events`            | 正典事件     | 正典イベント         | Canon events                 |
| `canon_relationships`     | 正典关系     | 正典関係           | Canon relationships          |

---

## 七、MCP 工具集 / MCP ツールセット / MCP Tool Set

### 7.1 工具清单 / ツール一覧 / Tool List

**CN**

| 分类       | 工具                                                                                                                                           | 用途                       |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| **项目管理** | `project_init` / `project_archive` / `project_delete` / `service_reset` / `project_export` / `project_import`                                | 项目 CRUD、重置、导入导出          |
| **章节**   | `chapter_sync` / `chapter_get` / `chapter_list`                                                                                              | 同步/获取/列出章节               |
| **人物**   | `character_save` / `character_status` / `character_snapshot` / `character_snapshot_check`                                                    | 保存画像、查状态、记录快照、检测影响       |
| **地点**   | `location_register` / `location_update` / `location_status`                                                                                  | 注册/更新/查询地点               |
| **物品**   | `item_register` / `item_update` / `item_query`                                                                                               | 注册/更新/查询物品详情             |
| **搜索**   | `rag_search` / `semantic_search` / `fuzzy_search`                                                                                            | 语义搜索+向量搜索+模糊搜索           |
| **图谱**   | `graph_query` / `graph_path`                                                                                                                 | 查询关系图、路径                 |
| **时间线**  | `timeline_create` / `timeline_event_add` / `timeline_event_update` / `timeline_check` / `timeline_link_create` / `timeline_link_query`       | 创建时间线、添加/更新事件、检查矛盾、关联时间线 |
| **推演**   | `deduce_behavior` / `deduce_outline` / `deduce_verify` / `register_foreshadowing`                                                            | 行为推演、大纲推演、验证、伏笔登记        |
| **正典**   | `canon_import` / `canon_character_add` / `canon_event_add` / `canon_relationship_add` / `canon_search` / `canon_verify` / `canon_status_set` | 正典导入、逐条录入、搜索、审核、走向追踪     |
| **宇宙**   | `universe_create` / `universe_list` / `universe_link`                                                                                        | 创建、列出、关联宇宙               |
| **语法检查** | `grammar_check`                                                                                                                              | LanguageTool 语法/错别字检查    |

**JP**

| 分類           | ツール                                                                                                           | 用途                            |
|--------------|---------------------------------------------------------------------------------------------------------------|-------------------------------|
| **プロジェクト管理** | `project_init` / `project_archive` / `project_delete` / `service_reset` / `project_export` / `project_import` | プロジェクト CRUD、リセット、インポート/エクスポート |
| **チャプター**    | `chapter_sync` / `chapter_get` / `chapter_list`                                                               | チャプターの同期/取得/一覧                |
| **キャラクター**   | `character_save` / `character_status` / `character_snapshot`                                                  | 設定保存、状態確認、スナップショット記録          |
| **ロケーション**   | `location_register` / `location_update` / `location_status`                                                   | ロケーションの登録/更新/照会               |
| **アイテム**     | `item_register` / `item_update` / `item_query`                                                                | アイテムの登録/更新/照会                 |
| **検索**       | `rag_search` / `semantic_search` / `fuzzy_search`                                                             | 意味検索＋ベクトル検索＋あいまい検索            |
| **グラフ**      | `graph_query` / `graph_path`                                                                                  | 関係グラフ照会、パス検索                  |
| **タイムライン**   | `timeline_create` / `timeline_event_add` / `timeline_check`                                                   | タイムライン作成、イベント追加、矛盾チェック        |
| **推論**       | `deduce_behavior` / `deduce_outline` / `deduce_verify` / `register_foreshadowing`                             | 行動推論、概要推論、検証、伏線登録             |
| **正典**       | `canon_import` / `canon_search` / `canon_verify`                                                              | 正典インポート/検索/確認                 |

**EN**

| Category         | Tools                                                                                                         | Purpose                                                      |
|------------------|---------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| **Project Mgmt** | `project_init` / `project_archive` / `project_delete` / `service_reset` / `project_export` / `project_import` | Project CRUD, reset, import/export                           |
| **Chapter**      | `chapter_sync` / `chapter_get` / `chapter_list`                                                               | Sync/get/list chapters                                       |
| **Character**    | `character_save` / `character_status` / `character_snapshot`                                                  | Save profile, check status, record snapshot                  |
| **Location**     | `location_register` / `location_update` / `location_status`                                                   | Register/update/query locations                              |
| **Item**         | `item_register` / `item_update` / `item_query`                                                                | Register/update/query items                                  |
| **Search**       | `rag_search` / `semantic_search` / `fuzzy_search`                                                             | Semantic + vector + fuzzy search                             |
| **Graph**        | `graph_query` / `graph_path`                                                                                  | Query relationship graph, paths                              |
| **Timeline**     | `timeline_create` / `timeline_event_add` / `timeline_check`                                                   | Create timeline, add events, check conflicts                 |
| **Deduction**    | `deduce_behavior` / `deduce_outline` / `deduce_verify` / `register_foreshadowing`                             | Behavior deduction, outline deduction, verify, foreshadowing |
| **Canon**        | `canon_import` / `canon_search` / `canon_verify`                                                              | Canon import/search/verify                                   |

### 7.2 工具原始需求 / ツール原初要件 / Tool Original Requirements

**CN**：每个工具的设计动机和输入输出详见 `development.md` §6。以下简称关键点：
- 所有工具通过 `POST /mcp` 调用，JSON-RPC 2.0 格式
- `project_id` 是大多数工具的必需参数
- 返回格式统一为 `record` 或 `String`

**JP**：各ツールの設計動機と入出力詳細は `development.md` §6 を参照。以下は要点のみ：
- 全ツールは `POST /mcp` 経由で呼び出し、JSON-RPC 2.0 形式
- `project_id` はほとんどのツールで必須パラメーター
- 戻り値は `record` または `String` で統一

**EN**：Design motivation and I/O details for each tool in `development.md` §6. Key points only:
- All tools called via `POST /mcp`, JSON-RPC 2.0 format
- `project_id` is required for most tools
- Return types unified as `record` or `String`

---

## 八、RAG 管线设计 / RAG パイプライン設計 / RAG Pipeline Design

### 8.1 流程 / フロー / Flow

**CN**

```
用户 query → bge-m3 embedding (Ollama on writing machine)
             → POST /api/rag/search (to Gateway)
                → pgvector cosine similarity search
                   → 返回 top-k 段落 + 元数据
                      → (可选) qwen2.5:3b 合成回答
```

**JP**

```
ユーザー query → bge-m3 embedding (ライティングマシンの Ollama)
               → POST /api/rag/search (Gatewayへ)
                  → pgvector コサイン類似度検索
                     → top-k パラグラフ + メタデータを返す
                        → (オプション) qwen2.5:3b で回答合成
```

**EN**

```
User query → bge-m3 embedding (Ollama on writing machine)
            → POST /api/rag/search (to Gateway)
               → pgvector cosine similarity search
                  → Return top-k paragraphs + metadata
                     → (Optional) qwen2.5:3b answer synthesis
```

### 8.2 关键参数 / 主要パラメーター / Key Parameters

| Parameter | Value | CN 说明 | JP 説明 | EN Explanation |
|-----------|-------|---------|---------|----------------|
| Embedding model | bge-m3 | 1024 维向量 | 1024次元ベクトル | 1024-dimensional vector |
| Top-k | 5-15 | 返回段落数 | 返却パラグラフ数 | Number of paragraphs returned |
| Similarity metric | cosine | pgvector 的 `<->` 操作符 | pgvector の `<->` 演算子 | pgvector `<->` operator |
| Chunk size | 500-800 chars | 分段大小 | 分割サイズ | Segmentation size |
| Chunk overlap | 100 chars | 相邻段重叠 | 隣接段落の重複 | Overlap between adjacent chunks |

### 8.3 RAG 合成 / RAG 合成 / RAG Synthesis

**CN**
- `rag_search` 默认不合成（`synthesize=false`），只返回段落列表
- 将 `synthesize=true` 传给 Gateway 后，Gateway 调 Ollama qwen2.5:3b 做合成
- 如果 qwen2.5:3b 不可用或效果不好，合成阶段跳过——客户端 AI 自行处理

**JP**
- `rag_search` はデフォルトでは合成しない（`synthesize=false`）、段落リストのみ返す
- `synthesize=true` を Gateway に渡すと、Gateway が Ollama qwen2.5:3b を呼び合成
- qwen2.5:3b が利用不可または効果が不十分な場合、合成はスキップ——クライアント AI が自前で処理

**EN**
- `rag_search` defaults to no synthesis (`synthesize=false`), returns only paragraph list
- When `synthesize=true` is passed, Gateway calls Ollama qwen2.5:3b for synthesis
- If qwen2.5:3b is unavailable or ineffective, synthesis is skipped — client AI handles it

---

## 九、推演引擎设计 / 推論エンジン設計 / Deduction Engine Design

### 9.1 核心定位 / 核心位置づけ / Core Positioning

**CN**
推演引擎不部署在 Gateway。它在 Reasonix（Claude）的写作帧内运行——不下放到服务器。Gateway 只负责为推演提供上下文数据。

**JP**
推論エンジンは Gateway にはデプロイしない。Reasonix（Claude）の執筆フレーム内で動作——サーバーには下ろさない。Gateway は推論にコンテキストデータを提供するだけ。

**EN**
The deduction engine is NOT deployed on Gateway. It runs inside Reasonix (Claude)'s writing frame — not offloaded to the server. Gateway only provides context data for deduction.

### 9.2 三个推演工具 / 三つの推論ツール / Three Deduction Tools

**CN**

| 工具 | 输入 | 输出 |
|------|------|------|
| `deduce_behavior` | 场景 + 角色列表 + 可选约束 | { 角色: { actions, dialogue, emotional_shift, voice_check }, ... } |
| `deduce_outline` | 章节目标 + 前情摘要 + 角色状态 | { scenes: [{ title, chars, key_events }], estimated_word_count } |
| `deduce_verify` | 推演输出 JSON | { consistency: bool, contradictions: [], voice_check: {} } |

**JP**

| ツール | 入力 | 出力 |
|--------|------|------|
| `deduce_behavior` | シーン＋キャラクターリスト＋オプション制約 | { キャラ: { actions, dialogue, emotional_shift, voice_check }, ... } |
| `deduce_outline` | チャプター目標＋前文要約＋キャラクター状態 | { scenes: [{ title, chars, key_events }], estimated_word_count } |
| `deduce_verify` | 推論出力 JSON | { consistency: bool, contradictions: [], voice_check: {} } |

**EN**

| Tool | Input | Output |
|------|-------|--------|
| `deduce_behavior` | Scene + character list + optional constraints | { char: { actions, dialogue, emotional_shift, voice_check }, ... } |
| `deduce_outline` | Chapter goal + prior context + character states | { scenes: [{ title, chars, key_events }], estimated_word_count } |
| `deduce_verify` | Deduction output JSON | { consistency: bool, contradictions: [], voice_check: {} } |

### 9.3 上下文组装 / コンテキスト構築 / Context Assembly

**CN**：推演前，Reasonix 先调 RAG 搜索和 `character_status` 获取上下文——包括：
- 前几章的相关段落（RAG 搜索）
- 各角色当前状态（`character_status`）
- 正典约束（`canon_search`，仅同人）
- 本章大纲（本地文件）

**JP**：推論前に、Reasonix は RAG 検索と `character_status` を呼び出してコンテキストを取得——以下を含む：
- 過去数章の関連パラグラフ（RAG 検索）
- 各キャラクターの現在状態（`character_status`）
- 正典制約（`canon_search`、二次創作のみ）
- 本章の概要（ローカルファイル）

**EN**：Before deduction, Reasonix calls RAG search and `character_status` to gather context:
- Relevant paragraphs from previous chapters (RAG search)
- Current character states (`character_status`)
- Canon constraints (`canon_search`, fanfic only)
- This chapter's outline (local file)

### 9.4 推演结果的应用 / 推論結果の活用 / Applying Deduction Results

**CN**
1. `deduce_behavior` 的结果展示给用户——用户决定采纳或修改
2. 用户确认后，按写作指纹生成正文
3. 正文完成后，调 `deduce_verify` 做一致性检查
4. 最后通过 `character_snapshot` 记录角色状态变更

**JP**
1. `deduce_behavior` の結果をユーザーに表示——ユーザーが採用か修正を決定
2. ユーザー確認後、執筆指針に従い本文を生成
3. 本文完成後、`deduce_verify` で一貫性チェック
4. 最後に `character_snapshot` でキャラクター状態変更を記録

**EN**
1. Show `deduce_behavior` results to the user — user decides to accept or modify
2. After user confirmation, generate prose following writing fingerprint
3. After completing prose, call `deduce_verify` for consistency check
4. Finally, record character state changes via `character_snapshot`

---

## 十、人物声纹 / キャラクター声紋 / Character Voiceprints

### 10.1 什么是声纹 / 声紋とは / What Are Voiceprints

**CN**：声纹是角色的说话方式——用词习惯、句式长度、语气特点、情绪表达模式。核心痛点：LLM 推演角色对话时，经常把所有人都写成同一种说话方式——"温柔地说"、"轻声问道"。声纹机制的目的是让每个角色说话不一样。

**JP**：声紋とはキャラクターの話し方——語彙の習慣、文の長さ、口調の特徴、感情表現パターン。核心の課題：LLM がキャラクターの会話を推論するとき、全員を同じ話し方にしてしまう——「優しく言う」「小声で尋ねる」。声紋メカニズムの目的は、各キャラクターの話し方を異なるものにすること。

**EN**：Voiceprints define a character's speech patterns — word choice, sentence length, tone, emotional expression. Core problem: LLMs tend to make all characters speak the same way — "gently said," "softly asked." The voiceprint mechanism ensures each character sounds distinct.

### 10.2 两种建立路径 / 二つの構築方法 / Two Setup Paths

**CN**

| 角色来源 | 声纹建立方式 | 初始数据 | 后续更新 |
|----------|-------------|---------|---------|
| **同人角色** | 从正典资料中提取该角色的标志性台词 | 手动录入 3-5 句经典台词 → 存入 `character_voiceprints` | 写新章后，自动提取本章对话追加 |
| **原创角色** | 作者先手写 2-3 句代表性对话作为"种子"；写完 5 章后，从正文自动提取对话增量更新 | 手写种子 → 存入 | 每章完成后自动追加 |

**JP**

| キャラクター由来 | 声紋構築方法 | 初期データ | 更新 |
|----------------|-------------|-----------|------|
| **二次創作キャラ** | 正典資料から当該キャラの特徴的な台詞を抽出 | 手動で 3-5 文の台詞を入力 → `character_voiceprints` に保存 | 新章執筆後、本章の会話を自動抽出して追加 |
| **オリジナルキャラ** | 作者が 2-3 文の代表的な会話を「シード」として手書き；5 章執筆後、本文から自動抽出で差分更新 | 手書きシード → 保存 | 各章完了後に自動追加 |

**EN**

| Character Source | Voiceprint Setup | Initial Data | Updates |
|-----------------|-----------------|-------------|---------|
| **Fanfic characters** | Extract iconic lines from canon material | Manually enter 3-5 classic lines → store in `character_voiceprints` | Auto-extract dialogue from new chapters |
| **Original characters** | Author hand-writes 2-3 representative dialogues as "seeds"; after 5 chapters, auto-extract from text | Hand-written seeds → store | Auto-append after each chapter |

**CN 核心原则**：声纹不需要一开始就完美。种子 + 每章自动增量 = 越写越准。

**JP 核心原則**：声紋は最初から完璧である必要はない。シード＋章ごとの自動増分＝書けば書くほど正確に。

**EN Core principle**: Voiceprints don't need to be perfect from the start. Seeds + auto-incremental per chapter = gets more accurate the more you write.

### 10.3 数据库设计 / データベース設計 / Database Design

**CN**：在 `characters` 表中扩展——加 `voice_seeds`（手写种子台词数组）和 `voice_meta`（声线硬约束 JSON）。另建 `character_voiceprints` 表存储提取的对话样本及向量。

**JP**：`characters` テーブルを拡張——`voice_seeds`（手書きシード台詞配列）と `voice_meta`（声線ハード制約 JSON）を追加。`character_voiceprints` テーブルで抽出した会話サンプルとベクトルを保存。

**EN**：Extend the `characters` table — add `voice_seeds` (hand-written seed dialogue array) and `voice_meta` (voice constraint JSON). Create `character_voiceprints` table for extracted dialogue samples and vectors.

```sql
ALTER TABLE characters ADD COLUMN voice_seeds TEXT[] DEFAULT '{}';
-- 示例: 凯妮: ["少废话。", "……（沉默）", "你他妈——"]

ALTER TABLE characters ADD COLUMN voice_meta JSONB DEFAULT '{}';
-- 示例:
-- {
--   "tone": "尖锐、粗糙、从不柔软",
--   "max_sentence_length": 15,
--   "forbidden_patterns": ["温柔地说", "轻声", "呢", "嘛"],
--   "default_behavior": "愤怒优先于解释。被触动时先沉默再回应",
--   "common_phrases": ["少废话", "开玩笑吧"],
--   "sentence_style": "90% 短句, 10% 沉默"
-- }
```

### 10.4 声纹在推演中的使用 / 声紋の推論での使用 / Voiceprints in Deduction

**CN**：三明治 Prompt——声线约束放在三个位置同时夹击：

1. 系统层：固定规则——"声纹是法律，违反声线的输出视为不合格"
2. 约束层：每个角色独立一段（来自 `voice_meta` + `voice_seeds`）
3. 输出指令层：再加一个具体限制（如"凯妮的台词不能超过 15 个字"）

**JP**：サンドイッチ Prompt——声線制約を三つの場所で同時に挟み撃ち：

1. システム層：固定ルール——「声紋は法律、違反出力は不合格」
2. 制約層：キャラクターごとに独立したセクション（`voice_meta` + `voice_seeds` から）
3. 出力指令層：さらに具体的制限（例：「凯妮の台詞は15文字以内」）

**EN**：Sandwich Prompt — voice constraints applied at three positions simultaneously:

1. System layer: Fixed rule — "Voiceprints are law. Violating voice constraints is considered invalid output"
2. Constraint layer: One section per character (from `voice_meta` + `voice_seeds`)
3. Output instruction layer: An additional specific limit (e.g., "Kainé's lines must not exceed 15 characters")

### 10.5 声纹自检 / 声紋自己チェック / Voiceprint Self-Check

**CN**：推演输出 JSON 加 `voice_check` 字段，让 LLM 自己检查自己的输出。`deduce_verify` 工具检查 `fails` 数组——有内容就标记为"声线不符，需人工确认"。

**JP**：推論出力 JSON に `voice_check` フィールドを追加し、LLM 自身に出力をチェックさせる。`deduce_verify` ツールが `fails` 配列を確認——内容があれば「声線不一致、要確認」とマーク。

**EN**：Add a `voice_check` field to deduction output JSON, letting the LLM self-check its own output. The `deduce_verify` tool inspects the `fails` array — if non-empty, marks as "voice mismatch, needs human review."

### 10.6 自动提取对话 / 自動会話抽出 / Auto Dialogue Extraction

**CN**：`chapter_sync` 的收尾步骤——正则匹配对话 → 关联说话人 → 去重 → 批量 embedding → 写入 `character_voiceprints`。自动提取的 `source='extracted'`，可信度低于种子和正典。

**JP**：`chapter_sync` の締めくくり——正規表現で会話をマッチ→話者を特定→重複除去→一括 embedding→`character_voiceprints` に書き込み。自動抽出の `source='extracted'` は信頼度がシードや正典より低い。

**EN**：Final step of `chapter_sync` — regex match dialogue → identify speaker → deduplicate → batch embedding → write to `character_voiceprints`. Auto-extracted `source='extracted'` has lower confidence than seeds and canon.

---

## 十一、项目隔离方案 / プロジェクト分離方案 / Project Isolation

**CN**

| 组件 | 隔离方式 | 操作 |
|------|---------|------|
| PostgreSQL | `project_id` 列 | WHERE 子句 + 外键 ON DELETE CASCADE |
| pgvector | 同上 | `WHERE project_id = $pid ORDER BY embedding <=> $vec` |
| 全文搜索 | 同上 | 列级别，天然隔离 |
| Neo4j | `project_id` 属性 | `WHERE n.project_id = $pid` |
| Ollama | 无状态 | 不存储项目数据，无需隔离 |

**JP**

| コンポーネント | 分離方法 | 操作 |
|--------------|---------|------|
| PostgreSQL | `project_id` カラム | WHERE 句＋外部キー ON DELETE CASCADE |
| pgvector | 同上 | `WHERE project_id = $pid ORDER BY embedding <=> $vec` |
| 全文検索 | 同上 | カラムレベル、天然の分離 |
| Neo4j | `project_id` プロパティ | `WHERE n.project_id = $pid` |
| Ollama | ステートレス | プロジェクトデータを保存しないため分離不要 |

**EN**

| Component | Isolation Method | Operations |
|-----------|-----------------|------------|
| PostgreSQL | `project_id` column | WHERE clause + FK ON DELETE CASCADE |
| pgvector | Same | `WHERE project_id = $pid ORDER BY embedding <=> $vec` |
| Full-text search | Same | Column-level, naturally isolated |
| Neo4j | `project_id` property | `WHERE n.project_id = $pid` |
| Ollama | Stateless | No project data stored, no isolation needed |

**CN 项目上限**：10 个项目以内——标签隔离完全够用。Neo4j 在几千节点级别没有任何性能问题。

**JP プロジェクト上限**：10 プロジェクト以内——タグ分離で十分。Neo4j は数千ノードレベルでは性能問題なし。

**EN Project Limit**: Up to 10 projects — tag isolation is sufficient. Neo4j has no performance issues at thousands of nodes.

---

## 十二、同人小说正典采集 / 二次創作正典収集 / Fanfic Canon Collection

### 12.1 原则 / 原則 / Principles

**CN**
- 只存储**事实性信息**：人名、时间、关系、事件概要
- 不存储原文段落
- 不收费——个人心愿创作
- 来源标注——方便追溯

**JP**
- **事実情報**のみ保存：人名、時間、関係、イベント概要
- 原文段落は保存しない
- 無料——個人の趣味創作
- 出典を明記——トレース容易に

**EN**
- Store only **factual information**: names, dates, relationships, event summaries
- Do NOT store original text paragraphs
- Free — personal passion project
- Source attribution for traceability

### 12.2 两层架构 / 二層アーキテクチャ / Two-Layer Architecture

**CN**

| 层 | 表 | 可信度 | 用途 |
|----|-----|--------|------|
| **正典层** | canon_characters, canon_events, canon_relationships | ⭐⭐⭐ 高（人工审核） | 推演红线 |
| **参考层** | 暂不建表，用文件记录 | ⭐ 低 | 灵感参考 |

**JP**

| 層 | テーブル | 信頼度 | 用途 |
|----|---------|--------|------|
| **正典層** | canon_characters, canon_events, canon_relationships | ⭐⭐⭐ 高（人為確認済み） | 推論の制約線 |
| **参照層** | 未作成、ファイルで記録 | ⭐ 低 | インスピレーション参照 |

**EN**

| Layer | Tables | Confidence | Purpose |
|-------|--------|------------|---------|
| **Canon layer** | canon_characters, canon_events, canon_relationships | ⭐⭐⭐ High (human-verified) | Deduction red lines |
| **Reference layer** | Not yet tables, file-based | ⭐ Low | Inspiration reference |

---

## 十三、复杂时间线建模 / 複雑なタイムラインモデリング / Complex Timeline Modeling

### 13.1 核心概念 / 核心概念 / Core Concepts

**CN**：每个事件有两个序号——`absolute_order`（故事内实际发生的时间顺序）和 `narrative_order`（读者感知的阅读顺序）。

**JP**：各イベントに二つの順序番号——`absolute_order`（ストーリー内の実際の時間順）と `narrative_order`（読者が知覚する読書順）。

**EN**：Each event has two order numbers — `absolute_order` (in-universe chronological order) and `narrative_order` (reader-perceived reading order).

### 13.2 图结构 / グラフ構造 / Graph Structure

```
(:Timeline {type: "main"})
  ├── (Event: 哈利收到信)  [absolute:1, narrative:1]
  ├── (Event: 对角巷)      [absolute:2, narrative:3]
  └── (Event: 分院仪式)    [absolute:3, narrative:2]

(:Timeline {type: "flashback"})
  └── (Event: 斯内普入学)  [absolute:-10, narrative:4]

(Event: 哈利被救)
  -[:CAUSED_BY {note:"时间闭环"}]-> (Event: 未来的哈利救自己)
```

**CN**：主时间线 + 回忆线 + 时间闭环，通过 Neo4j 关系连接。

**JP**：メインタイムライン＋回想線＋時間ループ、Neo4j 関係で接続。

**EN**：Main timeline + flashback line + time loop, connected via Neo4j relationships.

### 13.2.1 物品图谱 / アイテムグラフ / Item Graph

```
(:Character {name:"哈利"})
  -[:OWNS]-> (:Item {name:"隐身衣", item_type:"神器"})
  -[:SEEKS]-> (:Item {name:"魔法石"})

(:Location {name:"古灵阁"})
  -[:CONTAINS]-> (:Item {name:"魔法石"})

(:Item {name:"格兰芬多宝剑"})
  -[:FORGED_BY]-> (:Character {name:"戈德里克·格兰芬多"})
  -[:RELATED_TO]-> (:Item {name:"分院帽"})
```

**CN**：物品作为 Neo4j 节点，与人物（OWNS/SEEKS/FORGED_BY）、地点（CONTAINS）、其他物品（RELATED_TO）构成关系图谱。`item_query`
返回当前图谱关联。

**JP**：アイテムを Neo4j ノードとして、キャラクター（OWNS/SEEKS/FORGED_BY）、ロケーション（CONTAINS）、他アイテム（RELATED_TO）と関係グラフを構成。
`item_query` が現在のグラフ関連を返す。

**EN**：Items as Neo4j nodes, forming a relationship graph with Characters (OWNS/SEEKS/FORGED_BY), Locations (CONTAINS),
and other Items (RELATED_TO). `item_query` returns current graph relations.

### 13.3 时间线检查 / タイムラインチェック / Timeline Check

**CN**：`timeline_check` 只做规则检查，不做自动判定——标记可能的矛盾点供作者确认：

- ✅ 同一人物在不同时间线存在→正常（回忆有年轻版）
- ⚠️ 因果环 A→B→C→A → 标记（可能是时间闭环）
- ⚠️ 人物在 absolute=N 时死亡，N+1 又出现 → 标记（可能是穿越）
- ⚠️ narrative 和 absolute 差距过大未标注 → 提醒

**JP**：`timeline_check` はルールチェックのみ、自動判定はしない——可能性のある矛盾点を作者に確認用にマーク：

- ✅ 同一人物が別のタイムラインに存在→正常（回想では若いバージョン）
- ⚠️ 因果ループ A→B→C→A → マーク（時間ループの可能性）
- ⚠️ 人物が absolute=N で死亡、N+1 で再出現 → マーク（タイムトラベルの可能性）
- ⚠️ narrative と absolute の差が大きいのに未標記 → 注意喚起

**EN**：`timeline_check` performs rule-based checks only, no auto-judgment — flags potential conflicts for author review:

- ✅ Same character exists in different timelines → OK (younger version in flashback)
- ⚠️ Causal loop A→B→C→A → flagged (possible time loop)
- ⚠️ Character dies at absolute=N, reappears at N+1 → flagged (possible time travel)
- ⚠️ Large gap between narrative and absolute without annotation → warning

---

## 十四、本地 CLI / ローカル CLI / Local CLI

### 14.1 Spring Shell 命令 / Spring Shell コマンド / Spring Shell Commands

```bash
shell:> project init --name "星辰之海" --type original --path ~/novel-vault/星辰之海
shell:> project list
shell:> project use --name "以你的名"
shell:> chapter list
shell:> chapter sync --file ~/novel-vault/以你的名/chapters/第0005章-xxx.md
shell:> sync full --project "以你的名"
shell:> rag ask "凯妮在第三章说了什么" --synthesize
shell:> deduc behavior --scene "集装箱内，凯妮听完大天蓬" --chars "凯妮,杜兰"
shell:> deduc outline --chapter 10 --goal "古月暴露身份"
shell:> timeline check
shell:> canon import --type text
```

**CN**：Spring Shell 注解式——所有命令共享 Service 层，和 MCP 工具是同一套逻辑。

**JP**：Spring Shell アノテーション方式——全コマンドが Service 層を共有し、MCP ツールと同じロジック。

**EN**：Spring Shell annotation-style — all commands share the Service layer, same logic as MCP tools.

```java
@ShellComponent
public class ProjectCommands {
    @ShellMethod("初始化新项目")
    public String projectInit(@ShellOption String name, @ShellOption String type) {
        Project p = projectService.create(name, type);
        return "项目已创建: " + p.getId();
    }
}
```

---

## 十五、分阶段路线图 / フェーズ別ロードマップ / Phase Roadmap

### Phase 0 — 骨架 / スケルトン / Skeleton（2-3 周 / 週 / weeks）

**CN**
- [ ] Docker Compose：PG16+pgvector, Neo4j5, Meilisearch, LanguageTool, Ollama
- [ ] PG 初始化脚本（仅启用 pgvector 扩展——不需要 zhparser）
- [ ] Meilisearch 索引初始化（`novel_chapters` 索引 + filterableAttributes）+ 定义降级 ILIKE 后备
- [ ] Ollama 拉取 bge-m3
- [ ] Spring Boot 项目骨架 + 基础表创建（Flyway）
- [ ] `project_init` / `chapter_sync`（含分段 + 批量 embedding → PG + Meilisearch 写入）
- [ ] `semantic_search` / `fuzzy_search`（Meilisearch 为主，PG ILIKE 后备）
- [ ] `grammar_check`（LanguageTool 集成——Phase 0 就做）
- [ ] `rag_search`（synthesize=false——仅返回段落）
- [ ] 本地 file-watcher 脚本（Python watchdog）
- [ ] Obsidian Vault 目录结构确认

**JP**
- [ ] Docker Compose：PG16+pgvector, Neo4j5, Meilisearch, LanguageTool, Ollama
- [ ] PG 初期化スクリプト（pgvector 拡張のみ有効化——zhparser は不要）
- [ ] Meilisearch 索引初期化（`novel_chapters` 索引 + filterableAttributes）+ ILIKE フォールバック定義
- [ ] Ollama で bge-m3 をプル
- [ ] Spring Boot プロジェクトスケルトン＋基本テーブル作成（Flyway）
- [ ] `project_init` / `chapter_sync`（分割＋一括 embedding → PG + Meilisearch 書き込み）
- [ ] `semantic_search` / `fuzzy_search`（Meilisearch 主、PG ILIKE 予備）
- [ ] `grammar_check`（LanguageTool 統合——Phase 0 で実施）
- [ ] `rag_search`（synthesize=false——段落のみ返却）
- [ ] ローカル file-watcher スクリプト（Python watchdog）
- [ ] Obsidian Vault ディレクトリ構成確認

**EN**
- [ ] Docker Compose: PG16+pgvector, Neo4j5, Meilisearch, LanguageTool, Ollama
- [ ] PG init script (enable pgvector only — no zhparser needed)
- [ ] Meilisearch index init (`novel_chapters` index + filterableAttributes) + define ILIKE fallback
- [ ] Pull bge-m3 via Ollama
- [ ] Spring Boot project skeleton + base tables (Flyway)
- [ ] `project_init` / `chapter_sync` (with segmentation + batch embedding → PG + Meilisearch write)
- [ ] `semantic_search` / `fuzzy_search` (Meilisearch primary, PG ILIKE fallback)
- [ ] `grammar_check` (LanguageTool integration — done in Phase 0)
- [ ] `rag_search` (synthesize=false — returns paragraphs only)
- [ ] Local file-watcher script (Python watchdog)
- [ ] Obsidian Vault directory structure confirmation

**CN Phase 0 验收标准**：在 Obsidian 里写一章 → 保存 → 几秒后可以用语义搜索搜到、用模糊搜索搜到、语法检查可用

**JP Phase 0 合格基準**：Obsidian で一章書く→保存→数秒後に意味検索で見つかる、あいまい検索で見つかる、文法チェックが使える

**EN Phase 0 Acceptance Criteria**: Write a chapter in Obsidian → save → within seconds, findable via semantic search, fuzzy search, and grammar check available

---

### Phase 1 — 图 + 人物 + 版本 / グラフ＋キャラ＋バージョン / Graph + Characters + Versions（2 周 / 週 / weeks）

**CN**
- [ ] Neo4j 节点/关系创建（`chapter_sync` 集成）
- [ ] `items` 表 + `:Item` Neo4j 节点创建（`item_register` 集成）
- [ ] `graph_query` / `graph_path`
- [ ] `character_save` / `character_snapshot` / `character_status`
- [ ] `chapter_versions` 版本历史
- [ ] `chapter_sync` 的 file_hash 增量判断（不变就跳过）
- [ ] Spring Shell CLI 基础命令

**JP**
- [ ] Neo4j ノード/関係作成（`chapter_sync` 統合）
- [ ] `items` テーブル＋`:Item` Neo4j ノード作成（`item_register` 統合）
- [ ] `graph_query` / `graph_path`
- [ ] `character_save` / `character_snapshot` / `character_status`
- [ ] `chapter_versions` バージョン履歴
- [ ] `chapter_sync` の file_hash 増分判定（変更なければスキップ）
- [ ] Spring Shell CLI 基本コマンド

**EN**
- [ ] Neo4j node/relationship creation (`chapter_sync` integration)
- [ ] `items` table + `:Item` Neo4j node creation (`item_register` integration)
- [ ] `graph_query` / `graph_path`
- [ ] `character_save` / `character_snapshot` / `character_status`
- [ ] `chapter_versions` version history
- [ ] `chapter_sync` file_hash incremental check (skip if unchanged)
- [ ] Spring Shell CLI basic commands

---

### Phase 2 — 推演引擎 / 推論エンジン / Deduction Engine（2-3 周 / 週 / weeks）

**CN**
- [ ] `deduce_behavior`（含上下文自动组装）
- [ ] `deduce_outline`
- [ ] `deduce_verify`（本地 RAG 验证）
- [ ] `deduction_logs` 写入 + 成本追踪
- [ ] Claude API 备选（model 参数切换）
- [ ] CLI 推演命令

**JP**
- [ ] `deduce_behavior`（コンテキスト自動構築含む）
- [ ] `deduce_outline`
- [ ] `deduce_verify`（ローカル RAG 検証）
- [ ] `deduction_logs` 書き込み＋コスト追跡
- [ ] Claude API 代替（model パラメーター切替）
- [ ] CLI 推論コマンド

**EN**
- [ ] `deduce_behavior` (with automatic context assembly)
- [ ] `deduce_outline`
- [ ] `deduce_verify` (local RAG verification)
- [ ] `deduction_logs` write + cost tracking
- [ ] Claude API alternative (model parameter switching)
- [ ] CLI deduction commands

---

### Phase 3 — 正典 + 时间线 / 正典＋タイムライン / Canon + Timeline（2 周 / 週 / weeks）

**CN**
- [ ] `canon_import`（手动 + Ollama 3B 半自动）
- [ ] `canon_search`（RAG）
- [ ] `timeline_create` / `timeline_event_add`
- [ ] `timeline_check`
- [ ] Neo4j 时间线关系（BELONGS_TO, CAUSED_BY, TIME_TRAVEL）

**JP**
- [ ] `canon_import`（手動＋Ollama 3B 半自動）
- [ ] `canon_search`（RAG）
- [ ] `timeline_create` / `timeline_event_add`
- [ ] `timeline_check`
- [ ] Neo4j タイムライン関係（BELONGS_TO, CAUSED_BY, TIME_TRAVEL）

**EN**
- [ ] `canon_import` (manual + Ollama 3B semi-auto)
- [ ] `canon_search` (RAG)
- [ ] `timeline_create` / `timeline_event_add`
- [ ] `timeline_check`
- [ ] Neo4j timeline relationships (BELONGS_TO, CAUSED_BY, TIME_TRAVEL)

---

### Phase 4 — 完善 / 完成化 / Polish（持续 / 継続的 / Ongoing）

**CN**
- [ ] `rag_search` 可选合成（synthesize=true）
- [ ] `items` 表 embedding 向量索引 + 语义搜索
- [ ] 全量恢复脚本（从本地文件全量重建 DB + Meilisearch 索引）
- [ ] Meilisearch 同义词词典（自定义角色名/地名别名映射）
- [ ] 推演质量调优
- [ ] 性能监控

**JP**
- [ ] `rag_search` オプション合成（synthesize=true）
- [ ] `items` テーブル embedding ベクトル索引＋意味検索
- [ ] 全量復元スクリプト（ローカルファイルから DB + Meilisearch 索引を全再構築）
- [ ] Meilisearch 同義語辞書（カスタムキャラクター名/地名エイリアス）
- [ ] 推論品質チューニング
- [ ] パフォーマンス監視

**EN**
- [ ] `rag_search` optional synthesis (synthesize=true)
- [ ] `items` table embedding vector index + semantic search
- [ ] Full recovery script (rebuild DB + Meilisearch index from local files)
- [ ] Meilisearch synonym dictionary (custom character/place name aliases)
- [ ] Deduction quality tuning
- [ ] Performance monitoring

---

## 文件清单 / ファイル一覧 / File Manifest

```
novel-weaver/
├── docs/
│   ├── design.md                          ← 本文件 / 本ファイル / This file
│   ├── development.md                     ← 开发文档 / 開発ドキュメント
│   ├── directory-spec.md                  ← 目录规范 / ディレクトリ仕様
│   └── schema-v1-flyway.sql              ← 数据库 DDL / DB DDL
├── docker/
│   ├── docker-compose.yml                 ← 全部服务 / 全サービス
│   ├── init-pg.sql                        ← 仅 pgvector 扩展 / pgvector 拡張のみ
│   └── gateway/
│       └── Dockerfile                     ← Spring Boot 镜像 / イメージ
├── gateway/                               ← Spring Boot 源码 / ソース
│   ├── src/main/java/...
│   └── src/main/resources/
│       └── db/migration/                  ← Flyway 建表脚本
├── watcher/                               ← 本地文件监控 / ローカルファイル監視
│   ├── sync_watcher.py                    ← 宿主机器运行 / ホストマシンで実行
│   └── requirements.txt
├── shell/                                 ← Spring Shell CLI
│   └── ...
└── README.md
```
