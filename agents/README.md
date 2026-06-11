# Novel Weaver Gateway — Client Configuration / 客户端配置 / クライアント設定

> **CN** 本目录包含各平台连接 NOVEL-MCP-SERVER 所需的配置文件和模板  
> **JP** このディレクトリには各プラットフォームが NOVEL-MCP-SERVER に接続するための設定ファイルとテンプレートが含まれています  
> **EN** This directory contains config files and templates for connecting to NOVEL-MCP-SERVER from various platforms

---

## Server / 服务器 / サーバー

```
http://192.168.88.10:8883/mcp
```

---

## Quick Start / 快速使用 / クイックスタート

**CN** 配置好连接后，直接对 AI 说"初始化项目"。  
**JP** 接続設定後、AI に「プロジェクトを初期化」と話しかけてください。  
**EN** After setup, tell your AI: "initialize project".

---

## Setup by Platform / 各平台配置 / プラットフォーム別設定

| Platform | How to |
|----------|--------|
| **Reasonix** | Copy `agents/reasonix/*.md` → `.reasonix/skills/` |
| **Claude Code (CLI)** | Merge `agents/claude/settings-template.json` → `.claude/settings.local.json` |
| **Claude Desktop (GUI)** | Merge `agents/claude/claude_desktop_config.template.json` → config path |

---

## Backup & Restore / 备份恢复 / バックアップと復元

| Direction | Tool |
|-----------|------|
| DB → JSON | `project_export` |
| JSON → DB | `project_import` |

---

## Templates / 模板 / テンプレート

`agents/templates/` — used by `project-init` wizard automatically.

---

## All 33 Tools / 全部工具 / 全ツール

```
项目管理/Project Mgmt: project_init / archive / delete / reset / export / import
章节/Chapter:         sync / list / get
人物/Character:       save / status / snapshot / snapshot_check
地点/Location:        register / update / status
宇宙/Universe:        create / list / link
搜索/Search:          fuzzy / rag / semantic
图/Graph:             query / path
时间线/Timeline:      create / event_add / check / link_create / link_query
正典/Canon:           import / search / verify / status_set
伏笔/Foreshadowing:   register
推演/Deduction:       outline / behavior / verify
```
