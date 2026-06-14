# 数据库表结构参考

> NOVEL-MCP-SERVER 使用 PostgreSQL 存储数据。
> 以下表结构可通过 MCP 工具自动管理，无需手动操作。
> 此文件仅作为参考和理解数据模型的文档。

---

## 表总览

| 表名                        | 对应 MCP 工具                                               | 核心用途             |
|---------------------------|---------------------------------------------------------|------------------|
| `chapters`                | `chapter_sync`、`chapter_get`                            | 章节正文和元数据         |
| `chapter_paragraphs`      | `rag_search`、`semantic_search`                          | 段落级语义索引          |
| `character_profiles`      | `character_save`                                        | 人物画像基础信息         |
| `character_snapshots`     | `character_snapshot`、`character_status`                 | 角色状态快照历史         |
| `foreshadowing_index`     | `register_foreshadowing`                                | 伏笔登记和回收追踪        |
| `timelines`               | `timeline_create`                                       | 时间线定义            |
| `timeline_events`         | `timeline_event_add`、`timeline_check`                   | 时间线事件和矛盾检测       |
| `canon_characters`        | `canon_import`、`canon_search`                           | 正典人物             |
| `canon_events`            | `canon_import`、`canon_search`                           | 正典事件             |
| `character_relationships` | `graph_query`、`graph_path`                              | 人物关系图            |
| `deduction_logs`          | `deduce_behavior`、`deduce_outline`                      | 推演记录             |
| `locations`               | `location_register`、`location_update`、`location_status` | 地点档案             |
| `items`                   | `item_register`、`item_update`、`item_query`              | 物品档案——外观、来源、归属历史 |
| `review_logs`             | （预留）                                                    | 审查记录             |

---

## 核心表结构

### chapters（章节）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID | 主键 |
| `project_id` | UUID | 所属项目 |
| `chapter_number` | int | 章节号 |
| `title` | text | 标题 |
| `content` | text | 正文 |
| `phase` | varchar | 所属阶段 |
| `status` | varchar | draft / review / final |
| `word_count` | int | 字数 |
| `created_at` / `updated_at` | timestamp | 时间戳 |

### character_profiles（人物画像）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID | 主键 |
| `project_id` | UUID | 所属项目 |
| `name` | text | 角色名 |
| `bio` | text | 简介 |
| `traits` | jsonb | 性格特征数组 |
| `voice` | text | 声线描述 |
| `voice_seeds` | text[] | 声线种子台词 |
| `voice_meta` | jsonb | 声线硬约束 |

### character_snapshots（角色状态快照）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID | 主键 |
| `character_id` | UUID | 对应人物 |
| `chapter_id` | UUID | 所属章节 |
| `location` | text | 物理位置 |
| `physical` | text | 生理状态 |
| `psychology` | text | 心理状态 |
| `items` | text[] | 当前携带物品 |
| `summary` | text | 本章角色动态概述 |

### foreshadowing_index（伏笔索引）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID | 主键 |
| `project_id` | UUID | 所属项目 |
| `code` | varchar | 伏笔编号（F001-F999） |
| `description` | text | 伏笔描述 |
| `f_type` | varchar | 类型：🔮情感/🎭身份/🎯事件/💡道具 |
| `planted_chapter` | int | 埋设章节号 |
| `status` | varchar | active / triggered / closed |

### locations（地点档案）

| 字段                  | 类型    | 说明                |
|---------------------|-------|-------------------|
| `id`                | UUID  | 主键                |
| `project_id`        | UUID  | 所属项目              |
| `name`              | text  | 地点名称              |
| `location_type`     | text  | 类型（自然场景/建筑/聚落/室内） |
| `canon_description` | text  | 正典/原始设定           |
| `actual_appearance` | text  | 实际外观              |
| `change_log`        | jsonb | 变更历史              |
| `current_status`    | text  | 当前状态              |

### items（物品档案）

| 字段                 | 类型           | 说明                                                             |
|--------------------|--------------|----------------------------------------------------------------|
| `id`               | UUID         | 主键                                                             |
| `project_id`       | UUID         | 所属项目                                                           |
| `name`             | text         | 物品名称                                                           |
| `item_type`        | text         | 类型（武器/信物/神器/日常品/其他）                                            |
| `description`      | text         | 外观与功能描述                                                        |
| `origin`           | text         | 来源（谁造的、哪发现的）                                                   |
| `significance`     | text         | 剧情意义/用途                                                        |
| `properties`       | jsonb        | 自定义属性（如魔法能力）                                                   |
| `current_holder`   | text         | 当前持有者名                                                         |
| `current_location` | text         | 当前位置                                                           |
| `current_status`   | text         | 当前状态（正常/损坏/遗失/销毁）                                              |
| `first_chapter`    | int          | 首次出现章节                                                         |
| `owner_history`    | jsonb        | 归属变更历史 `[{"chapter":1,"from":"...","to":"...","event":"..."}]` |
| `embedding`        | vector(1024) | bge-m3 向量（语义搜索）                                                |

---

## 与项目2 SQLite 的对应关系

如果你是从 project2 的 SQLite 结构迁移过来，以下是表名映射：

| project2 SQLite | MCP PostgreSQL | 差异说明 |
|----------------|---------------|---------|
| `chapters` | `chapters` | 基本一致，MCP 多了 phase 和 word_count |
| `canon_events` | `canon_events` | 基本一致 |
| `canon_event_status` | → 合并到 `timeline_events` | MCP 用时间线事件 + `timeline_check` 替代 |
| `character_states` | `character_snapshots` | 结构类似，MCP 每章可记录多个角色 |
| `kaine_defense_layers` | → 项目自定义字段 | 此类项目专用追踪数据建议写在 `character_snapshots.psychology` 或独立文件中 |
| `derivation_log` | `deduction_logs` | MCP 的推演工具自动生成 |
| `location_details` | → 建议放在 locations/ 文件中 | 地点描写细节不适合结构化存储 |
| `original_scenes` | → 合并到 `chapters` 的 content 中 | 不单独成表 |
