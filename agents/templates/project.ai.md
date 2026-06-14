# project.ai.md — {项目中文名}

> 类型：{original / fanfic} | 主角：{主角名} | 叙事视角：{POV}
> 体裁：{网文风格 / 传统文学 / ...}
> 🔴 **MCP 项目ID**：`{项目ID}` ← 初始化时自动替换为实际 UUID；若仍为占位符，请从 `project.yaml` 的 `mcp_project_id` 读取

---

## 一、每次会话必须做的事

**在写任何大纲或正文之前，先执行以下步骤（🔴 强制顺序）：**

### 步骤 A：读本地文件

```
🔴 outlines/ch{NNNN}-{标题}.md         ← 【最优先】章节大纲是权威来源
🔴 characters/{主角名}.md              ← 主角设定
🔴 characters/{重要角色}.md            ← 本章涉及的关键角色
🔴 project.yaml                        ← 🔴 读取 mcp_project_id（后续所有 MCP 调用需要）
style/tone.md                          ← 文风设定（声线规范、禁止事项）
style/prompts.md                       ← 推演模板（卡文时使用）
canon/timeline.md（同人小说）          ← 确认正典时间线位置
states/current.yaml                    ← 确认各人物"当前位置"
foreshadowing.yaml                     ← 检查是否有需要回收的伏笔
items/{物品名}.md                       ← 本章涉及的关键物品（如有）
```

**🔴 最优先规则：章节大纲 > 画像文件。** 大纲有明确规定的，以大纲为准。

### 步骤 B：查 MCP（获取最新数据上下文）

调以下 MCP 工具拉取数据库中的最新状态（客户端本地调 Ollama bge-m3 做 embedding，不走远程 LLM）：

```reasonix
# 语义搜索已有正文——获取与当前章节相关的已写内容
mcp__novel-mcp-server__rag_search
  projectId: "{项目ID}"
  query: "{要写的内容关键词或场景描述}"
  k: 5

# 查询本章涉及的角色的最新状态
mcp__novel-mcp-server__character_status
  projectId: "{项目ID}"
  name: "{角色名}"

# 检查活跃伏笔（推演工具已自动包含）
mcp__novel-mcp-server__deduce_behavior
  projectId: "{项目ID}"
  charNames: ["{主角}", "{重要角色}"]
  scene: "{本章场景描述}"
```

> **🔴 `rag_search` 必须调。** 写作前不查已有内容等于不知道之前写了什么。
> character_status 根据本章涉及的角色调，至少调主角的。
> devise_behavior 按需调用，卡文时必调。

---

## 二、写作铁律

1. **角色性格不可偏移** — 每个角色有清晰的性格边界，不能因为剧情需要就让人物做不符合性格的事
2. **文风保持一致** — 简洁、有画面感、不堆砌形容词。参照 `style/tone.md`
3. **不违背世界规则** — 世界观文件中的硬约束绝对不可打破
4. **伏笔要有出处** — 每个伏笔必须能追溯到设定文件中的某个设定点
5. **不提前剧透** — 角色只能基于自己知道的信息行动，禁止全知视角泄露
6. **主角不能无脑变强** — 不能突然获得奇遇、用嘴炮解决战斗、龙傲天展开
7. **禁止 AI 式叙事** — 全部以人类口语写作：
   - 禁止排比句堆砌（"他看到了希望，看到了光明，看到了未来"）
   - 禁止空洞抒情升华（"在这一刻，他终于明白了什么是真正的……"）
   - 禁止过度使用"仿佛""宛如""如同"等比喻连接词
   - 禁止每段结尾来一句总结性的感悟金句
   - 禁止华丽但空洞的形容词堆砌氛围（"寂寥的月光洒在苍茫的大地上"）
   - 禁止角色内心独白写成散文诗
   - 禁止对话后面跟一大段解释角色"此刻心中涌起了……"
   - **正确的写法**：像一个人在跟朋友讲故事那样写。用大白话、短句、具体动作代替抽象描写
8. **禁用轻小说翻译腔** — "呐""嘛""呢"等。说话用 `""`，内心想法用 `『』`

---

## 三、章节规范

- 每章 {min}-{max} 字
- 每章有一个**核心场景**（推动剧情或关系的关键事件）
- 每章至少有一个**小细节**（展示角色非主线的人性化一面）
- **章末留悬念或情绪钩子**

---

## 四、工作流程

> 🔴 完整流程参见 `agents/reasonix/chapter-write.md`（Reasonix）或 `agents/claude/chapter-write.md`（Claude）。
> 以下是日常写作时的核心步骤速查。

### 写大纲时

1. **查数据库** — `rag_search`（语义搜索已有内容）+ `deduce_outline`（推演大纲）
2. **读本地** — `outlines/总纲.md` + `characters/` + `items/` + `locations/` + `foreshadowing.yaml` +
   `canon/timeline.md`
3. **读模板** — `agents/templates/chapter-outline.md`
4. **写大纲** → 标注伏笔来源和叙事目的 → 写入 `outlines/ch{NNNN}-{标题}.md`
5. **用户确认** → 定稿后（可选）`chapter_sync` 记录大纲版本

### 写正文时

1. **查数据库** — `rag_search` + `character_status`（所有出场角色）+ `item_query`（涉及物品）+ `location_status`（涉及地点）+
   `timeline_check` + `deduce_behavior`（卡文时）
2. **读本地** — 🔴 大纲 `outlines/ch{NNNN}.md` 最优先 → `characters/` → `items/` → `locations/` → `states/current.yaml` →
   `foreshadowing.yaml` → `style/tone.md`
3. **写作** — 按大纲场景顺序，用 `{lang}` 语言，遵守写作铁律
4. **写入** — `chapters/ch{NNNN}-{标题}.md`
5. **审核** — 反AI检查 + 角色一致性 + 字数（参照 `style/review.md`）

### 每章写完后（🔴 必须）

1. **同步正文** — `chapter_sync`（含 characters/items 列表，客户端本地 bge-m3 embedding）
2. **记录状态** — 每个出场角色调一次 `character_snapshot`
   🔴 `summary` 由 AI 从已写内容提取该角色本章动态（1-2 句），不需要额外调 LLM
3. **登记伏笔** — 有新伏笔 → `register_foreshadowing`
4. **更新物品** — 物品状态变化 → `item_update`
5. **更新地点** — 地点状态变化 → `location_update`
6. **检查时间线** — `timeline_check`
7. **更新本地** — `states/ch{NNNN}.yaml` + `states/current.yaml` + `foreshadowing.yaml`
8. **字数核对** — 确认在 {min}-{max} 范围内

### 修改时

1. `rag_search` + `chapter_get` → 拉当前章节
2. 读 `chapters/ch{NNNN}.md` + 相关设定
3. 改正文（本地）
4. `chapter_sync`（生成新版本）
5. 人物状态变了 → `character_snapshot`

### 关键原则

| 原则          | 说明                                 |
|-------------|------------------------------------|
| **大纲优先**    | 大纲有规定的以大纲为准                        |
| **先查后写**    | rag_search + character_status 必须先调 |
| **写完即审**    | 审核是步骤的一部分                          |
| **一次性入库**   | 整章写完再批量调 MCP                       |
| **本地+DB双写** | chapters/states 与 MCP 数据库同步更新      |

---

## 五、可用 MCP 工具

> 🔴 **projectId 来源**：所有 MCP 工具调用中的 `projectId` 从 `project.yaml` 的 `mcp_project_id` 字段读取。
> 如果 CLI 中看到 `{项目ID}` 占位符，说明初始化时未正确替换——手动从 `project.yaml` 复制 `mcp_project_id` 的值。

> 🔴 **Embedding 关键规则**：`rag_search`、`semantic_search`、`chapter_sync` 的向量由**写作机本地 Ollama** 生成。
> 模型名称和地址在项目根目录的 `ollama-config.yaml` 中配置（与 `project.yaml` 同级，默认 `embedding_model: bge-m3`，
`ollama_host: localhost`，`ollama_port: 11434`）。
> MCP 服务端不调任何 LLM，只存向量和做 pgvector 检索。
>
> **调用方式**：
> ```
> POST http://{ollama_host}:{ollama_port}/api/embed
> {"model":"{embedding_model}","input":"要向量化的文本"}
> → 返回 {"embeddings":[[0.123, -0.456, ...]]} → 取 embeddings[0]
> ```
>
> - `rag_search`/`semantic_search`：对**查询文本**调 `/api/embed`，结果传入 `embedding` 参数
> - `chapter_sync`：正文分段（500-800字/段）→ 每段调 `/api/embed` → 向量列表传入 `embeddings` 参数
>
> 🟡 **可选：轻量摘要模型** — 如果 `ollama-config.yaml` 中配置了 `summary_model`（如 `qwen2.5:3b`）：
> ```
> POST http://{ollama_host}:{ollama_port}/api/generate
> {"model":"{summary_model}","prompt":"为角色{角色名}在本章的表现写一行摘要","stream":false}
> → 返回的 response 字段作为 summary
> ```
> 如果 `summary_model` 为空，AI 自行从已写内容提取 1-2 句。
> 效果不好时直接去掉这个能力，不影响核心功能。

| 工具                       | 用途                           | 何时调用           |    需要 embedding    |
|--------------------------|------------------------------|----------------|:------------------:|
| `rag_search`             | 语义搜索已写内容                     | 🔴 **写作前必调**   | ✅ 查询文本 → embedding |
| `semantic_search`        | 纯向量搜索                        | 需要精确向量匹配时      | ✅ 查询文本 → embedding |
| `fuzzy_search`           | 关键词模糊搜索                      | 查角色/地点在哪章出现    |         ❌          |
| `deduce_behavior`        | 推演角色行为                       | 卡文时辅助（自动拉取上下文） |         ❌          |
| `deduce_outline`         | 推演章节大纲                       | 大纲不清晰时         |         ❌          |
| `chapter_sync`           | 同步章节到 DB                     | 🔴 **每章写完后必调** | ✅ 段落文本 → embedding |
| `character_snapshot`     | 记录人物状态（summary 由 AI 从已写内容提取） | 🔴 **每章写完后必调** |         ❌          |
| `register_foreshadowing` | 登记伏笔                         | 有新伏笔时          |         ❌          |
| `item_query`             | 查询物品详情+关联图谱                  | 涉及重要物品时        |         ❌          |
| `item_register`          | 注册新物品                        | 引入新物品时         |         ❌          |
| `item_update`            | 更新物品状态/持有者                   | 物品归属变化时        |         ❌          |
| `location_status`        | 查询地点状态                       | 涉及特定地点时        |         ❌          |
| `location_update`        | 更新地点状态                       | 地点发生变化时        |         ❌          |
| `timeline_check`         | 检查时间线矛盾                      | 每章写完后          |         ❌          |

---

## 六、人物速查

| 人物 | 状态 | 声线关键词 | 当前章节 |
|------|------|-----------|---------|
| {主角} | ✅ | | |
| {角色2} | ✅ | | |
| {角色3} | | | |

详细画像见 `characters/` 目录。当前状态见 `states/current.yaml`。

---

## 七、阶段速查

| 阶段 | 章节 | 核心任务 | 约束 |
|------|------|---------|------|
| {阶段1} | {N-N} | {任务描述} | {约束} |
| {阶段2} | {N-N} | {任务描述} | {约束} |
