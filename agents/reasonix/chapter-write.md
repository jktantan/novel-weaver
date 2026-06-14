---
name: chapter-write
description: 章节写作完整流程——写大纲/写正文的自动化闭环（查DB→读本地→整合→写作→审核→入库）
runAs: inline
---

# 章节写作流程

当用户说"写大纲""写第N章""写正文"或类似意思时，按对应流程执行。

## 前置步骤：读配置文件

🔴 在任何流程开始之前，先读取配置文件：

```
read_file project.yaml          ← 🔴 获取 mcp_project_id（后续所有 MCP 调用需要）
read_file ollama-config.yaml
```

从配置中提取以下变量（文件不存在则用默认值）：

| 变量                  | 配置字段              | 来源                   | 默认值         |
|---------------------|-------------------|----------------------|-------------|
| `{projectId}`       | `mcp_project_id`  | `project.yaml`       | （无，必须存在）    |
| `{embedding_model}` | `embedding_model` | `ollama-config.yaml` | `bge-m3`    |
| `{summary_model}`   | `summary_model`   | `ollama-config.yaml` | （空，用 AI 提取） |
| `{ollama_host}`     | `ollama_host`     | `ollama-config.yaml` | `localhost` |
| `{ollama_port}`     | `ollama_port`     | `ollama-config.yaml` | `11434`     |

后续步骤中所有 `{projectId}`、`{embedding_model}`、`{summary_model}`、`{ollama_host}`、`{ollama_port}` 都从配置取值。

## 流程 A：写大纲

触发词：「写大纲」「生成第N章大纲」「outline」

### 1. 查数据库

按顺序调用：

> 🔴 **Embedding 前置步骤**：`rag_search` 需要 `embedding` 参数。
> 在调 `rag_search` 之前，先用配置中的 embedding 模型生成查询向量：
>
> ```
> POST http://{ollama_host}:{ollama_port}/api/embed
> {"model":"{embedding_model}","input":"{本章要写的场景或关键词}"}
> ```
>
> 从返回的 JSON 中提取 `["embeddings"][0]` 向量数组，作为 `rag_search` 的 `embedding` 参数。

```
# 语义搜索已有正文 → 与本章相关的已写内容
mcp__novel-mcp-server__rag_search
  projectId: "{项目ID}"
  query: "{本章要写的场景或关键词}"
  embedding: "{上一步 {embedding_model} 返回的向量}"
  k: 8

# 推演大纲（如果有前因/后果约束）
mcp__novel-mcp-server__deduce_outline
  projectId: "{项目ID}"
  mode: 2                    # 2=因定果不定（推荐）
  premise: "{本章前因}"
  characters: ["{主角}", "{涉及角色}"]
```

### 2. 读本地文件

🔴 按顺序读：

```
read_file outlines/总纲.md               ← 故事整体框架（如有）
read_file outlines/ch{NNNN}-{标题}.md    ← 已有大纲（修改时）
read_file characters/{主角名}.md          ← 主角设定
read_file characters/{涉及角色}.md       ← 本章相关角色
read_file items/{物品名}.md              ← 本章关键物品（如有）
read_file locations/{地点名}.md          ← 本章主要地点（如有）
read_file foreshadowing.yaml             ← 埋了哪些伏笔、哪些该回收
read_file canon/timeline.md              ← （同人）正典时间线
```

### 3. 读模板

```
read_file {WEAVER_HOME}/agents/templates/chapter-outline.md
```

### 4. 生成大纲

整合以上信息，用 `{lang}` 语言生成大纲。必须包含：

- **章节标题**
- **场景列表**（每个场景标注：地点、出场人物、核心事件）
- **伏笔**（本章埋下的 + 本章回收的，标注来源）
- **叙事目的**（本章推动什么、展示什么）
- **情绪曲线**（本章的情绪走向）

### 5. 写入本地文件

```
write_file outlines/ch{NNNN}-{标题}.md
```

### 6. 等待用户确认

输出大纲摘要，等用户说"可以"或"改一下XX"后再进入下一步。

### 7. （可选）同步到数据库

如果用户想记录大纲版本：

```
mcp__novel-mcp-server__chapter_sync
  projectId: "{项目ID}"
  number: {章节号}
  title: "{标题}"
  content: "{大纲内容}"
  phase: "{阶段名}"
```

---

## 流程 B：写正文

触发词：「写第N章」「开始写」「draft」

### 1. 查数据库

> 🔴 **Embedding 前置步骤**：`rag_search` 和 `semantic_search` 需要 `embedding` 参数。
> 在调用之前，先用配置中的 embedding 模型生成查询向量：
>
> ```
> POST http://{ollama_host}:{ollama_port}/api/embed
> {"model":"{embedding_model}","input":"{本章核心场景描述}"}
> ```
>
> 从返回 JSON 中提取 `["embeddings"][0]` 作为 `embedding` 参数。

```
# 语义搜索已有正文
mcp__novel-mcp-server__rag_search
  projectId: "{项目ID}"
  query: "{本章核心场景描述}"
  embedding: "{{embedding_model} 返回的向量}"
  k: 8

# 角色最新状态
mcp__novel-mcp-server__character_status
  projectId: "{项目ID}"
  name: "{主角}"
  # identity: {"generation": 1}  ← 同名角色时必传

mcp__novel-mcp-server__character_status
  projectId: "{项目ID}"
  name: "{重要角色}"

# 涉及物品
mcp__novel-mcp-server__item_query
  projectId: "{项目ID}"
  name: "{物品名}"

# 涉及地点
mcp__novel-mcp-server__location_status
  projectId: "{项目ID}"
  name: "{地点名}"

# 检查时间线矛盾
mcp__novel-mcp-server__timeline_check
  projectId: "{项目ID}"

# 推演行为（卡文时、或涉及复杂人际互动时）
mcp__novel-mcp-server__deduce_behavior
  projectId: "{项目ID}"
  charNames: ["{主角}", "{重要角色}"]
  scene: "{本章场景描述}"
```

### 2. 读本地文件

🔴 按顺序读：

```
🔴 read_file outlines/ch{NNNN}-{标题}.md  ← 【最优先】大纲是权威来源
read_file characters/{主角}.md
read_file characters/{角色2}.md
read_file items/{物品名}.md
read_file locations/{地点名}.md
read_file states/current.yaml
read_file foreshadowing.yaml
read_file style/tone.md                   ← 文风/声线约束
```

### 3. 写作

按以下约束生成正文（用 `{lang}` 语言）：

- 严格按大纲的场景顺序写
- 对话用 `""`，内心用 `『』`
- 遵守 `style/tone.md` 的声线规范
- 遵守 §二 写作铁律（不偏移性格、保持文风、不剧透等）
- 字数：{min}-{max} 字
- 章末留悬念或情绪钩子

### 4. 写入本地文件

```
write_file chapters/ch{NNNN}-{标题}.md
```

### 5. 审核

🔴 写完后必须执行：

**a) 反 AI 检查**（对照 `style/review.md`）：

- 排比句堆砌
- 空洞抒情升华
- 过度比喻连接词
- 段落结尾金句
- 角色内心独白散文诗化

**b) 角色一致性**：

- 说的话是否符合角色性格和声线
- 做的事是否符合角色动机

**c) 字数核对**：

- 是否在 {min}-{max} 范围内

### 6. 入库（MCP 同步）

写完后一次性同步到数据库：

> 🔴 **Embedding 前置步骤**：`chapter_sync` 的 `embeddings` 参数可选但建议传入。
> 将正文按 ~500-800 字分段，每段调用配置中的 embedding 模型：
>
> ```
> POST http://{ollama_host}:{ollama_port}/api/embed
> {"model":"{embedding_model}","input":"{段落文本}"}
> ```
>
> 将各段返回的向量按顺序组成列表，传入 `chapter_sync` 的 `embeddings` 参数。
> 如果不传 embeddings，服务端仍会保存章节，但段落级语义搜索将不可用。

```
# ① 同步章节正文
mcp__novel-mcp-server__chapter_sync
  projectId: "{项目ID}"
  number: {N}
  title: "{标题}"
  content: "{正文}"
  phase: "{阶段}"
  characters: ["{出场角色列表}"]
  items: ["{出场物品列表}"]
  locations: ["{出场地点列表}"]
  embeddings: ["{段落1向量}", "{段落2向量}", ...]

# ② 记录人物状态快照
# 🔴 summary 逻辑：
#    如果 ollama-config.yaml 中配置了 summary_model（如 qwen2.5:3b）：
#      POST http://{ollama_host}:{ollama_port}/api/generate
#      {"model":"{summary_model}","prompt":"为角色{角色名}在本章中的表现写一行摘要（{lang}）","stream":false}
#      → 取返回的 response 字段作为 summary
#    如果 summary_model 为空或文件不存在：
#      AI 直接从已写内容中提取该角色本章动态（1-2 句）
mcp__novel-mcp-server__character_snapshot
  projectId: "{项目ID}"
  chapterNumber: {N}
  name: "{主角}"
  location: "{位置}"
  physical: "{生理状态}"
  psychology: "{心理状态}"
  items: ["{持有物品}"]
  summary: "{本章角色动态}"

# （每个出场角色都调一次 character_snapshot）

# ③ 登记新伏笔
mcp__novel-mcp-server__register_foreshadowing
  projectId: "{项目ID}"
  code: "F{编号}"
  description: "{伏笔内容}"
  fType: "🔮情感/🎭身份/🎯事件/💡道具"
  plantedChapter: {N}
  characters: ["{涉及角色}"]

# ④ 更新物品状态（如有变化）
mcp__novel-mcp-server__item_update
  projectId: "{项目ID}"
  name: "{物品名}"
  chapter: {N}
  event: "{事件（赠予/遗失/发现）}"
  newHolder: "{新持有者}"
  newStatus: "{新状态}"

# ⑤ 更新地点状态（如有变化）
mcp__novel-mcp-server__location_update
  projectId: "{项目ID}"
  name: "{地点名}"
  chapter: {N}
  triggerEvent: "{触发事件}"
  change: "{变化内容}"
  newStatus: "{新状态}"

# ⑥ 时间线矛盾检查
mcp__novel-mcp-server__timeline_check
  projectId: "{项目ID}"

# ⑦ 语法检查
mcp__novel-mcp-server__grammar_check
  text: "{正文}"
  language: "zh-CN"
# 如有问题修复后重新 sync

# ⑧ 正典事件走向（仅同人项目）
# 如果本章触发/修改/跳过了正典事件，逐条记录：
mcp__novel-mcp-server__canon_status_set
  projectId: "{项目ID}"
  canonEventId: "{正典事件ID}"
  status: "triggered/modified/skipped"
  occurredInChapter: {N}
  actualDescription: "{实际发生的情况}"
  divergenceReason: "{偏离原因}"
```

### 7. 更新本地文件

```
# 更新人物状态文件
write_file states/ch{NNNN}.yaml

# 更新 current.yaml（始终反映最新一章的状态）
write_file states/current.yaml

# 更新伏笔文件（如有新伏笔或回收）
write_file foreshadowing.yaml
```

---

## 流程 C：修改已写章节

触发词：「修改第N章」「改一下XX」

1. **查数据库** — `rag_search` + `chapter_get` 拉当前章节
2. **读本地** — `read_file chapters/ch{NNNN}.md` + 相关设定
3. **改正文** — 只在本地改
4. **重新同步** — `chapter_sync`（会生成新版本）
5. **更新快照** — 如果人物状态变了 → `character_snapshot`

---

## 关键原则

| 原则            | 说明                                                                                       |
|---------------|------------------------------------------------------------------------------------------|
| **大纲优先**      | 大纲有明确规定的，以大纲为准，不可偏离                                                                      |
| **先查后写**      | rag_search + character_status 必须先调                                                       |
| **写完即审**      | 审核不是可选的，是步骤的一部分                                                                          |
| **一次性入库**     | 不要写一段同步一段，整章写完再一次性调用所有 MCP 工具                                                            |
| **本地+DB双写**   | 本地文件（chapters/、states/）和 MCP 数据库必须同步更新                                                   |
| **同名角色**      | 如项目有同名实体（如尼尔双子），`character_save`/`character_status`/`item_query` 等传 `identity` JSON 精确匹配 |
| **{lang} 语言** | 所有生成内容用项目初始化时选择的写作语言                                                                     |

---

## Embedding 说明 / Summary 说明

### Ollama Embedding 调用

模型由项目根目录的 `ollama-config.yaml` 中的 `embedding_model` 决定（默认 `bge-m3`）。

```
POST http://{ollama_host}:{ollama_port}/api/embed
{"model":"{embedding_model}","input":"要向量化的文本"}
→ 返回 {"embeddings":[[...]]} → 取 embeddings[0]
```

| 用于                             | 何时生成                       |
|--------------------------------|----------------------------|
| `rag_search(embedding=...)`    | 每次搜索前，对查询文本调 `/api/embed`  |
| `chapter_sync(embeddings=...)` | 正文写完后，分段(500-800字)逐段调，列表传入 |

### Summary 摘要

| 条件                  | 行为                                                                           |
|---------------------|------------------------------------------------------------------------------|
| `summary_model` 已配置 | `POST /api/generate {"model":"{summary_model}","prompt":"..."}` → `response` |
| `summary_model` 为空  | AI 从已写内容提取 1-2 句                                                             |

同步写入 `states/current.yaml` 和 `states/ch{NNNN}.yaml`。
