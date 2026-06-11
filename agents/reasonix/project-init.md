---
name: project-init
description: 交互式项目初始化向导——问几个问题，自动生成项目结构和项目规则，创建 MCP 项目
runAs: inline
---

# 项目初始化向导

当用户说"初始化项目"或类似意思时，按以下步骤执行。

## 第一步：收集信息（逐个问用户）

依次问以下问题，每问一个等用户回答再问下一个：

1. **项目名称** — 中文全名
2. **项目类型** — `original`（原创）还是 `fanfic`（同人）
3. **主角** — 主角名字
4. **叙事视角** — 例如"主角名第三人称有限"
5. **风格基准** — 例如"某部作品名"，不填就跳过
6. **每章字数** — 最小和最大（默认 5000-9000）
7. **简单描述** — 一句话说清这是个什么故事

## 第二步：创建 MCP 项目

```reasonix
mcp__novel-mcp-server__project_init
  name: "{项目名}"
  type: "{类型}"
  meta: { "protagonist": "{主角}", "pov": "{视角}", "description": "{描述}" }
```

记录返回的 `projectId`。

## 第三步：生成项目规则文件

用 `agents/templates/project.ai.md` 的格式填充用户信息，生成项目根目录的 `CLAUD.md`（Reasonix 会在 session 加载时读取它作为项目规则）。

如果找不到模板，直接按以下结构生成：

```
# {项目名}
> 类型：{类型} | 主角：{主角} | 叙事视角：{视角}

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

用 `agents/templates/project-meta.yaml` 的格式生成 `project.yaml`。

创建以下目录和文件：

| 目录/文件 | 来源 | 说明 |
|-----------|------|------|
| `CLAUD.md` | `agents/templates/project.ai.md` 填充 | 项目规则 |
| `project.yaml` | `agents/templates/project-meta.yaml` 填充 | 项目元数据 |
| `style/tone.md` | `agents/templates/tone.md` 复制 | 文风设定（需填写） |
| `style/prompts.md` | `agents/templates/prompts.md` 复制 | 推演模板（无需修改） |
| `style/review.md` | `agents/templates/review.md` 复制 | 审查模板（无需修改） |
| `locations/` | `agents/templates/location.md` | 地点档案（首次出现新地点时创建） |
| `outlines/` | 新建 | 大纲目录 |
| `chapters/` | 新建 | 章节目录 |
| `characters/` | 新建 | 人物目录 |

`db-schema.md` 是参考文档，按需查阅，不需要复制到项目中。

## 第五步：告知用户

```
✅ 项目初始化完成！

项目ID: {projectId}
已创建:
  CLAUD.md          — 项目规则
  project.yaml   — 项目元数据
  style/tone.md         — 文风设定（需填入角色声线）
  style/prompts.md      — 推演模板
  style/review.md       — 审查模板
  locations/            — 地点档案目录
  outlines/      — 大纲目录
  chapters/      — 章节目录
  characters/    — 人物目录

接下来你想做什么？
  1. 生成大纲
  2. 创建人物
  3. 直接开始写
```
