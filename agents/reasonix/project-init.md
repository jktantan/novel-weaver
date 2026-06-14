---
name: project-init
description: 交互式项目初始化向导——问几个问题，自动生成项目结构和项目规则，创建 MCP 项目
runAs: inline
---

# 项目初始化向导

当用户说"初始化项目"或类似意思时，按以下步骤执行。

## 步骤〇：定位模板文件

在开始之前，你需要知道 novel-weaver 项目（即本网关服务器项目）在你的机器上放在哪里。
模板文件都在该项目的 `agents/templates/` 目录下。

**用 `{lang}` 语言问用户：**

> "Where is the novel-weaver project located on your machine? (e.g. C:\Users\you\novel-weaver, /home/you/novel-weaver)"

记下路径为 `{WEAVER_HOME}`。

**再问：**

> "Where is your novel vault? (e.g. ~/novel-vault, C:\Users\you\novels)"

记下路径为 `{VAULT_HOME}`。这是小说仓库的根目录，所有项目目录都在这里。

**重要**：后续所有 `agents/templates/xxx.md` 路径都是相对 `{WEAVER_HOME}` 的。Ollama 配置文件放在项目目录下（与
`project.yaml` 同级），通过相对路径 `ollama-config.yaml` 读取。
在生成每个文件之前，**必须先用文件读取工具（如 `read_file`）读取对应模板**，然后再按模板格式生成。

## 第一步：收集信息（逐个问用户）

### 0. 写作语言（首个问题——唯一用英文问的）

First, ask in English:

> "What language will this novel be written in? (e.g. zh-CN, ja, en, ko, fr, de…)"

用户回答后，**后续所有对话、问题、生成的提示信息、文件内容全部使用该语言**。

例如：

- 用户答 `ja` → 后续问题用日文问：「プロジェクト名は？」、「主人公は誰？」
- 用户答 `zh-CN` → 后续问题用中文问（当前默认行为）
- 用户答 `en` → 后续问题用英文问

将此语言代码记作 `{lang}`。

---

从第 1 题开始，用 `{lang}` 语言提问：

1. **项目名称** — 用 `{lang}` 语言取名
2. **项目类型** — `original`（原创）还是 `fanfic`（同人）
3. **主角** — 主角名字
4. **叙事视角** — 例如「主角名第三人称有限」
5. **风格基准** — 例如「某部作品名」，不填就跳过
6. **每章字数** — 最小和最大（默认 5000-9000）
7. **简单描述** — 一句话说清这是个什么故事

## 第二步：创建 MCP 项目

```reasonix
mcp__novel-mcp-server__project_init
  name: "{项目名}"
  type: "{类型}"
  meta: { "protagonist": "{主角}", "pov": "{视角}", "description": "{描述}", "language": "{lang}" }
```

记录返回的 `projectId`。

## 第三步：生成项目规则文件

用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/project.ai.md` 的内容，按格式填充用户信息，生成项目根目录的 `CLAUD.md`
（Reasonix 会在 session 加载时读取它作为项目规则）。

**🔴 关键规则**：`CLAUD.md` 的全部内容（标题、说明文字、约束描述）一律用 `{lang}` 语言撰写。

如果找不到模板，直接按以下结构生成（内容用 `{lang}` 语言）：

```
# {项目名}
> 类型：{类型} | 主角：{主角} | 叙事视角：{视角}
> 写作语言：{lang}

## 写作约束
- 章节字数：{min}-{max} 字
- 视角：{视角}
- 对话用 ""，内心用 『』

## 每章写作后流程
1. 更新人物状态
2. 反向核对人物画像
3. chapter_sync 同步到 MCP
```

## 第四步：生成项目文件和目录

### 4.1 `project.yaml`

用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/project-meta.yaml` 的格式生成，关键字段：

- `name` — 用 `{lang}` 语言的项目名称
- `language` — 填入 `{lang}`（如 `zh-CN`、`ja`、`en`）
- 所有注释、字段值用 `{lang}` 语言填写

### 4.2 创建目录和文件

**🔴 所有生成文件的标题、说明、占位文本一律使用 `{lang}` 语言。**
**生成每个文件前，先用 `read_file` 读取对应模板，再按模板格式生成。**

| 目录/文件                | 模板（读取 `{WEAVER_HOME}/` 下的路径）                        | 说明                              |
|----------------------|-----------------------------------------------------|---------------------------------|
| `CLAUD.md`           | `read_file agents/templates/project.ai.md` → 填充     | 项目规则（`{lang}` 语言）               |
| `project.yaml`       | `read_file agents/templates/project-meta.yaml` → 填充 | 项目元数据（`{lang}` 语言）              |
| `ollama-config.yaml` | 从 `{WEAVER_HOME}/ollama-config.example.yaml` 复制     | Ollama 模型配置（按需修改模型名）            |
| `style/tone.md`      | `read_file agents/templates/tone.md` → 复制并填写        | 文风设定（用 `{lang}` 填写）             |
| `style/prompts.md`   | `read_file agents/templates/prompts.md` → 复制并填写     | 推演模板（用 `{lang}` 填写）             |
| `style/review.md`    | `read_file agents/templates/review.md` → 复制并填写      | 审查模板（用 `{lang}` 填写）             |
| `locations/`         | `read_file agents/templates/location.md`            | 地点档案（每地点一个 `.md`，用 `{lang}` 填写） |
| `items/`             | `read_file agents/templates/item.md`                | 物品档案（每物品一个 `.md`，用 `{lang}` 填写） |
| `characters/`        | `read_file agents/templates/character-profile.md`   | 人物目录（每人一个 `.md`，用 `{lang}` 填写）  |
| `outlines/`          | `read_file agents/templates/chapter-outline.md`     | 大纲目录（用 `{lang}` 填写）             |
| `chapters/`          | `read_file agents/templates/chapter-draft.md`       | 章节目录（用 `{lang}` 填写）             |

**章节标题格式按 `{lang}` 调整：**

- `zh-CN` → `第{NNNN}章 {标题}`
- `ja` → `第{NNNN}話 {タイトル}`
- `en` → `Chapter {N}: {Title}`
- 其他语言按惯例

`db-schema.md` 是参考文档，按需查阅，不需要复制到项目中。

> 🟡 **Ollama 配置**：从 `{WEAVER_HOME}/ollama-config.example.yaml` 复制到项目目录下的 `ollama-config.yaml`（与
`project.yaml` 同级），按需修改模型名。

## 第五步：告知用户

用 `{lang}` 语言输出：

```
✅ 项目初始化完成！

项目ID: {projectId}
已创建:
  CLAUD.md              — 项目规则
  project.yaml          — 项目元数据
  style/tone.md         — 文风设定（需填入角色声线）
  style/prompts.md      — 推演模板
  style/review.md       — 审查模板
  locations/            — 地点档案目录
  items/                — 物品档案目录
  outlines/             — 大纲目录
  chapters/             — 章节目录
  characters/           — 人物目录

接下来你想做什么？
  1. 生成大纲（详见 `agents/reasonix/chapter-write.md` 流程 A）
  2. 创建人物
  3. 直接开始写（详见 `agents/reasonix/chapter-write.md` 流程 B）
```
