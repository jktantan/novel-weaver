---
name: mcp-tools
description: NOVEL-MCP-SERVER 工具调用参考
runAs: inline
---

# MCP 工具调用参考

## 项目

```reasonix
mcp__novel-mcp-server__project_init
  name: "项目名"
  type: "fanfic"

mcp__novel-mcp-server__project_delete
  projectId: "<项目ID>"

mcp__novel-mcp-server__service_reset
```

## 章节

```reasonix
mcp__novel-mcp-server__chapter_sync
  projectId: "<项目ID>"
  number: 1
  title: "标题"
  content: "# 正文..."

mcp__novel-mcp-server__chapter_get
  projectId: "<项目ID>"
  number: 1
```

## 人物

```reasonix
mcp__novel-mcp-server__character_save
  projectId: "<项目ID>"
  name: "角色名"
  bio: "简介"
  traits: ["特征1","特征2"]

mcp__novel-mcp-server__character_snapshot
  projectId: "<项目ID>"
  name: "角色名"
  chapterNumber: 1
  location: "地点"
  physical: "生理状态"
  psychology: "心理状态"
```

## 时间线

```reasonix
mcp__novel-mcp-server__timeline_create
  projectId: "<项目ID>"
  name: "主时间线"
  type: "main"

mcp__novel-mcp-server__timeline_event_add
  projectId: "<项目ID>"
  timelineId: "<时间线ID>"
  name: "事件名"
  absoluteOrder: 1
```

## 伏笔

```reasonix
mcp__novel-mcp-server__register_foreshadowing
  projectId: "<项目ID>"
  code: "F001"
  description: "伏笔描述"
  fType: "🔮情感"
  plantedChapter: 1
```

## 搜索

```reasonix
mcp__novel-mcp-server__fuzzy_search
  projectId: "<项目ID>"
  keyword: "关键词"
```

## 推演

```reasonix
mcp__novel-mcp-server__deduce_behavior
  projectId: "<项目ID>"
  charNames: ["角色A","角色B"]
  scene: "场景描述"
```

## 备份与恢复

```reasonix
mcp__novel-mcp-server__project_export
  projectId: "<项目ID>"

mcp__novel-mcp-server__project_import
  projectId: "<项目ID>"
  jsonData: "{...从 export 得到的完整 JSON ...}"
```

## 宇宙管理

```reasonix
mcp__novel-mcp-server__universe_create
  projectId: "<项目ID>"
  name: "尼尔正史宇宙"
  type: "fanfic"

mcp__novel-mcp-server__universe_list
  projectId: "<项目ID>"

mcp__novel-mcp-server__universe_link
  projectId: "<项目ID>"
  fromUniverseId: "..."
  toUniverseId: "..."
  relationType: "parallel_to"
```

## 地点管理

```reasonix
mcp__novel-mcp-server__location_register
  projectId: "<项目ID>"
  name: "地点名"
  locationType: "建筑"
  canonDescription: "初始描述"
  actualAppearance: "叙事视角看到的"

mcp__novel-mcp-server__location_update
  projectId: "<项目ID>"
  name: "地点名"
  chapter: 5
  triggerEvent: "发生了什么"
  change: "具体变化"
  newStatus: "当前状态"

mcp__novel-mcp-server__location_status
  projectId: "<项目ID>"
  name: "地点名"
```
