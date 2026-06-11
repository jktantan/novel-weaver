# 项目初始化向导 — Claude 版

当用户说"初始化项目"或"新建项目"时，按以下流程执行。

## 步骤一：收集信息

逐个问用户，每次只问一个：

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
  meta: { "protagonist": "{主角}", "pov": "{视角}", "description": "{描述}" }
```

记下返回的 projectId。

## 步骤三：生成项目文件

1. 按 `agents/templates/project.ai.md` 的格式生成 `CLAUD.md`（Claude 自动读取作为项目规则）
2. 按 `agents/templates/project-meta.yaml` 的格式生成 `project.yaml`
3. 复制 `agents/templates/tone.md` 到 `style/tone.md`（需填写角色声线）
4. 复制 `agents/templates/prompts.md` 到 `style/prompts.md`（不需修改）
5. 复制 `agents/templates/review.md` 到 `style/review.md`（不需修改）
6. 创建目录：`outlines/`、`chapters/`、`characters/`、`locations/`
   （首次出现新地点时用 `agents/templates/location.md` 创建地点档案）

找不到模板时，直接参考以下结构：

**项目规则文件结构**：
```
# {项目名}
> 类型：{类型} | 主角：{主角} | 叙事视角：{视角}
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
protagonist: "{主角}"
pov: "{视角}"
writing_constraints:
  chapter_word_count: { min: {min}, max: {max} }
```

## 步骤四：告知用户

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
  outlines/
  chapters/
  characters/

接下来你想？
  1. 写大纲
  2. 先建人物
  3. 写第一章
```
