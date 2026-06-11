# Templates 使用说明 — Reasonix

`agents/templates/` 下的模板由 `/project-init` 向导自动使用，通常不需要手动操作。

如果你要手动创建项目文件，参考以下对应关系：

| 模板 | 生成文件 | 说明 |
|------|---------|------|
| `project.ai.md` | 项目规则文件（如 CLAUDE.md） | 项目规则和写作约束 |
| `project-meta.yaml` | `project.yaml` | 结构化元数据 |
| `tone.md` | `style/tone.md` | 文风设定和角色声线（需填写） |
| `prompts.md` | `style/prompts.md` | 推演模板（直接可用） |
| `review.md` | `style/review.md` | 审查模板（直接可用） |
| `location.md` | `locations/{地点名}.md` | 地点档案（用 MCP `location_register` 写入 DB，文件作为备份） |
| `db-schema.md` | 参考文档，不自动生成 | 数据库表结构说明 |
| `chapter-outline.md` | `outlines/` | 每章大纲格式 |
| `chapter-draft.md` | `chapters/` | 每章正文格式 |
| `character-profile.md` | `characters/` | 人物画像格式 |
