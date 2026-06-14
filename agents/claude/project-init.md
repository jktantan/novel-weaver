# 项目初始化向导 — Claude 版

当用户说"初始化项目"或"新建项目"时，按以下流程执行。

## 步骤〇：定位模板文件

在开始之前，你需要知道 novel-weaver 项目在你的机器上的路径。
模板文件都在该项目的 `agents/templates/` 目录下。

**询问用户：**

> "Where is the novel-weaver project located on your machine? (e.g. C:\Users\you\novel-weaver, /home/you/novel-weaver)"

记下路径为 `{WEAVER_HOME}`。

**再问：**

> "Where is your novel vault? (e.g. ~/novel-vault, C:\Users\you\novels)"

记下路径为 `{VAULT_HOME}`。

后续步骤中，所有 `agents/templates/xxx.md` 路径都是 `{WEAVER_HOME}/agents/templates/xxx.md`。
Ollama 配置路径是 `ollama-config.yaml`（项目根目录，与 `project.yaml` 同级）。
**在生成每个文件之前，先用文件读取工具读取对应模板。**

## 步骤一：收集信息

### 0. 写作语言（首个问题——唯一用英文问的）

First, ask in English:

> "What language will this novel be written in? (e.g. zh-CN, ja, en, ko, fr, de…)"

用户回答后，**后续所有对话、问题、生成的提示信息、文件内容全部使用该语言**。

将此语言代码记作 `{lang}`。

---

用 `{lang}` 语言逐个问用户，每次只问一个：

1. **项目名称**
2. **类型**：原创还是同人？
3. **主角是谁？**
4. **叙事视角？**（例如"主角第三人称有限"）
5. **风格参考？**（可选，不填跳过）
6. **每章字数范围？**（默认 5000-9000）
7. **一句话描述？**

## 步骤二：创建 MCP 项目

```
mcp__novel-mcp-server__project_init
  name: "{项目名}"
  type: "{original | fanfic}"
  meta: { "protagonist": "{主角}", "pov": "{视角}", "description": "{描述}", "language": "{lang}" }
```

记下返回的 projectId。

## 步骤三：生成项目文件

**🔴 所有生成文件的标题、说明、占位文本一律使用 `{lang}` 语言。**
**🔴 生成每个文件前，必须先用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/xxx.md`。**

1. 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/project.ai.md` → 按格式生成 `CLAUD.md`（Claude 自动读取作为项目规则）——
   **全文用 `{lang}` 语言**
2. 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/project-meta.yaml` → 按格式生成 `project.yaml`——`name` 用
   `{lang}` 语言，`language` 字段填入 `{lang}`
3. 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/tone.md` → 复制到 `style/tone.md`（用 `{lang}` 填写角色声线）
4. 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/prompts.md` → 复制到 `style/prompts.md`（用 `{lang}` 填写提示文本）
5. 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/review.md` → 复制到 `style/review.md`（用 `{lang}` 填写审查标准）
6. 创建目录：`outlines/`、`chapters/`、`characters/`、`locations/`、`items/`
7. 复制 `{WEAVER_HOME}/ollama-config.example.yaml` → `ollama-config.yaml`（按需修改模型名）
    - 首次出现新地点 → 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/location.md` → 创建地点档案（`{lang}` 语言）
    - 首次出现新物品 → 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/item.md` → 创建物品档案（`{lang}` 语言）
    - 首次出现新角色 → 用 `read_file` 读取 `{WEAVER_HOME}/agents/templates/character-profile.md` → 创建人物画像（`{lang}`
      语言）

**章节标题格式按 `{lang}` 调整：**

- `zh-CN` → `第{NNNN}章 {标题}`
- `ja` → `第{NNNN}話 {タイトル}`
- `en` → `Chapter {N}: {Title}`

找不到模板时，直接参考以下结构（内容用 `{lang}` 语言）：

**项目规则文件结构**：
```
# {项目名}
> 类型：{类型} | 主角：{主角} | 叙事视角：{视角}
> 写作语言：{lang}
## 写作约束
- 字数：{min}-{max}
- 对话 ""，内心 『』
## 每章写作后
1. 更新人物状态
2. chapter_sync 同步到 MCP
```

**project.yaml 结构**：
```yaml
name: "{项目名}"
language: "{lang}"
protagonist: "{主角}"
pov: "{视角}"
writing_constraints:
  chapter_word_count: { min: {min}, max: {max} }
```

## 步骤四：告知用户

用 `{lang}` 语言输出：

```
✅ 项目初始化完成！

项目ID: {projectId}
已创建:
  CLAUD.md
  project.yaml
  style/tone.md
  style/prompts.md
  style/review.md
  locations/
  items/
  outlines/
  chapters/
  characters/

接下来你想？
  1. 写大纲（详见 `agents/claude/chapter-write.md` 流程 A）
  2. 先建人物
  3. 写第一章（详见 `agents/claude/chapter-write.md` 流程 B）
```
