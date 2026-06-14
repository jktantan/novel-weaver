# 章节写作流程 — Claude 版

当用户说"写大纲""写第N章""写正文"时，按对应流程执行。

## 前置步骤：读配置文件

🔴 先读项目根目录下的配置文件：

```
read_file project.yaml          ← 🔴 获取 mcp_project_id（后续所有 MCP 调用需要）
read_file ollama-config.yaml
```

| 变量                  | 字段                | 来源                   | 默认值         |
|---------------------|-------------------|----------------------|-------------|
| `{projectId}`       | `mcp_project_id`  | `project.yaml`       | （无，必须存在）    |
| `{embedding_model}` | `embedding_model` | `ollama-config.yaml` | `bge-m3`    |
| `{summary_model}`   | `summary_model`   | `ollama-config.yaml` | （空）         |
| `{ollama_host}`     | `ollama_host`     | `ollama-config.yaml` | `localhost` |
| `{ollama_port}`     | `ollama_port`     | `ollama-config.yaml` | `11434`     |

后续步骤中所有 `{projectId}` 从 `project.yaml` 取值，`{embedding_model}` 等从 `ollama-config.yaml` 取值。

---

## 流程 A：写大纲

触发：「写大纲」「生成第N章大纲」

### 1. 查数据库

> 🔴 `rag_search` 需要 `embedding` 参数。用配置中的 embedding 模型生成：
> `POST http://{ollama_host}:{ollama_port}/api/embed` → `{"model":"{embedding_model}","input":"{查询文本}"}`
> → 取返回的 `embeddings[0]` 作为 `embedding` 参数。

```
mcp__novel-mcp-server__rag_search
  projectId: "{项目ID}"
  query: "{本章场景关键词}"
  embedding: "{{embedding_model} 向量}"
  k: 8

mcp__novel-mcp-server__deduce_outline
  projectId: "{项目ID}"
  mode: 2
  premise: "{本章前因}"
  characters: ["{主角}", "{涉及角色}"]
```

### 2. 读本地文件

按顺序读取：

- `outlines/总纲.md`（如有）
- `characters/{主角}.md`、`characters/{涉及角色}.md`
- `items/{物品名}.md`（本章关键物品）
- `locations/{地点名}.md`（本章主要地点）
- `foreshadowing.yaml`
- `canon/timeline.md`（同人）
- `{WEAVER_HOME}/agents/templates/chapter-outline.md`（模板）

### 3. 生成大纲

整合信息，用 `{lang}` 语言写大纲。必须包含：

- 场景列表（地点、人物、事件）
- 伏笔标记（埋下/回收，标注来源）
- 叙事目的
- 情绪曲线

### 4. 写入本地 → 用户确认

```
write_file outlines/ch{NNNN}-{标题}.md
```

等用户确认后再进入下一步。

---

## 流程 B：写正文

触发：「写第N章」「开始写」

### 1. 查数据库

> 🔴 每次调 `rag_search` 前先调配置中的 embedding 模型：
> `POST http://{ollama_host}:{ollama_port}/api/embed` → `{"model":"{embedding_model}","input":"{查询}"}`
> → `embeddings[0]` 作为 MCP 的 `embedding` 参数。

```
rag_search       → 语义搜索已有正文（k=8） ← 需要 {embedding_model} embedding
character_status → 主角 + 所有出场角色
item_query       → 本章涉及物品
location_status  → 本章涉及地点
timeline_check   → 时间线矛盾检查
deduce_behavior  → 推演角色行为（卡文时/复杂互动时必调）
```

### 2. 读本地文件

🔴 严格按此顺序：

```
🔴 outlines/ch{NNNN}-{标题}.md   ← 【最优先】大纲权威来源
characters/{主角}.md
characters/{角色}.md
items/{物品}.md
locations/{地点}.md
states/current.yaml
foreshadowing.yaml
style/tone.md                    ← 文风/声线
```

### 3. 写作

- 严格按大纲场景顺序
- 对话 `""`，内心 `『』`
- 遵守写作铁律（不偏移性格、保持文风、不剧透）
- 字数 {min}-{max}
- 章末留悬念/情绪钩子
- 用 `{lang}` 语言

### 4. 写入本地

```
write_file chapters/ch{NNNN}-{标题}.md
```

### 5. 审核

按 `style/review.md` 检查：

- 反 AI 痕迹（排比句、空洞抒情、金句结尾）
- 角色一致性（说的话/做的事是否符合性格）
- 字数范围

### 6. 入库（一次批量同步）

> 🔴 `chapter_sync` 可传 `embeddings`（段落向量列表）。
> 正文分段（500-800字/段）→ 每段调 `POST /api/embed {"model":"{embedding_model}","input":"..."}` → 向量列表传入
`embeddings`。

```
chapter_sync          → 同步章节正文 + characters/items + embeddings 列表
character_snapshot    → 每个出场角色一次
                        🔴 summary：如果配置了 summary_model 则调 /api/generate；否则 AI 自提取
register_foreshadowing → 有新伏笔时
item_update           → 物品状态变化时
location_update       → 地点状态变化时
timeline_check        → 再次确认时间线无矛盾
```

### 7. 更新本地文件

```
write_file states/ch{NNNN}.yaml       ← 本章人物状态
write_file states/current.yaml         ← 始终是最新状态
write_file foreshadowing.yaml          ← 伏笔更新
```

---

## 流程 C：修改已写章节

1. `rag_search` + `chapter_get` → 拉当前章节
2. `read_file chapters/ch{NNNN}.md` + 相关设定
3. 改正文（只在本地）
4. `chapter_sync`（生成新版本）
5. 人物状态变了 → `character_snapshot`

---

## 关键原则

- **大纲优先**：大纲规定的内容不可偏离
- **先查后写**：rag_search + character_status 必须先调
- **写完即审**：审核是写作步骤的一部分
- **一次性入库**：整章写完再批量调 MCP，不要写一段同步一段
- **本地+DB双写**：chapters/ 和 states/ 必须与 MCP 数据库同步
- **同名角色**：如项目有同名实体（如不同宇宙的同一人物），`character_save`/`character_status` 等传 `identity` JSON 精确匹配
- **{lang} 语言**：全部内容用项目初始化时选择的写作语言

---

## Embedding / Summary 速查

模型由项目根目录的 `ollama-config.yaml` 配置（默认 embedding: `bge-m3`）。

### Ollama 调用

```
POST http://{ollama_host}:{ollama_port}/api/embed
{"model":"{embedding_model}","input":"要向量化的文本"}
→ 返回 {"embeddings":[[...]]} → 取 embeddings[0]
```

| 用于                             | 何时生成                       |
|--------------------------------|----------------------------|
| `rag_search(embedding=...)`    | 每次搜索前，对查询文本调 `/api/embed`  |
| `chapter_sync(embeddings=...)` | 正文写完后，分段(500-800字)逐段调，列表传入 |

### Summary（摘要）

`character_snapshot(summary=...)` — 行为取决于配置：

| 条件                  | 行为                                                                                          |
|---------------------|---------------------------------------------------------------------------------------------|
| `summary_model` 已配置 | `POST /api/generate {"model":"{summary_model}","prompt":"为{角色}写本章摘要（{lang}）"}` → `response` |
| `summary_model` 为空  | AI 从已写内容提取 1-2 句                                                                            |

同步写入 `states/current.yaml` 和 `states/ch{NNNN}.yaml`。
