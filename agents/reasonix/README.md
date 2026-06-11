# Reasonix 配置 — NOVEL-MCP-SERVER

## 快速开始

```reasonix
/project-init
```

或直接说"初始化项目"。

## 安装

将 `agents/reasonix/` 下所有 `.md` 文件复制到项目 `.reasonix/skills/`：

```bash
cp agents/reasonix/*.md 你的项目/.reasonix/skills/
```

## 可用命令

| 命令 | 用途 |
|------|------|
| `/project-init` | 交互式项目初始化 |
| `/mcp-tools` | 查看 MCP 工具调用示例 |

## 全部工具

```
项目管理:   project_init / project_archive / project_delete / service_reset
章节管理:   chapter_sync / chapter_list / chapter_get
人物管理:   character_save / character_status / character_snapshot
搜索:       fuzzy_search / rag_search / semantic_search
图查询:     graph_query / graph_path
时间线:     timeline_create / timeline_event_add / timeline_check
正典:       canon_import / canon_search / canon_verify
伏笔:       register_foreshadowing
推演:       deduce_outline / deduce_behavior / deduce_verify
备份恢复:   project_export / project_import
地点管理:   location_register / location_update / location_status
宇宙管理:   universe_create / universe_list / universe_link
```

> 审查由 AI agent 自身完成（Reasonix 用 `/reviewer`，Claude 直接描述需求）。

## 数据备份与恢复

| 方向 | 操作 |
|------|------|
| DB → 文件 | `mcp__novel-mcp-server__project_export` 获取 JSON → AI 写入本地文件 |
| 文件 → DB | AI 读取本地文件组装 JSON → `mcp__novel-mcp-server__project_import` 恢复 |

文件和数据库互为镜像，任一损坏可从另一方恢复。
