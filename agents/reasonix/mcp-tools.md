---
name: mcp-tools
description: NOVEL-MCP-SERVER 工具调用参考（45 个工具）
runAs: inline
---

# MCP 工具调用参考

> 当前版本：45 个 MCP 工具，覆盖项目管理、章节、人物、物品、地点、宇宙、时间线、正典、搜索、图谱、推演。
> 🔴 **projectId 来源**：所有工具中的 `<项目ID>` 从项目根目录 `project.yaml` 的 `mcp_project_id` 字段读取。初始化时由
`project_init` 返回后自动写入。
>
> 🔴 **同名实体区分（identity）**：`character_save`、`item_register`、`location_register` 等工具支持可选的 `identity` JSON
> 参数。用于区分同一项目内的同名实体（如尼尔的双子姐妹、平行世界同一地点）。
>
> ```reasonix
> # 例：注册第二组 Devola
> mcp__novel-mcp-server__character_save
>   projectId: "<项目ID>"
>   name: "Devola"
>   identity: {"generation": 2, "assigned_to": "海岸镇"}
> ```
>
> - 不传 `identity`：同名且唯一时正常工作；同名多条时返回 `allProfiles` 列表
> - 传 `identity`：精确匹配，创建/更新/查询指定变体
> - `identity` 内容由项目自行定义，不同项目可用不同字段（如`{"generation":1}`、`{"era":"黄金时代"}`、
    `{"clone_id":"CT-7567"}`）

## 项目

```reasonix
mcp__novel-mcp-server__project_init
  name: "项目名"
  type: "fanfic"      // original | fanfic

mcp__novel-mcp-server__project_archive
  projectId: "<项目ID>"

mcp__novel-mcp-server__project_delete
  projectId: "<项目ID>"

mcp__novel-mcp-server__service_reset
  // 清空全部数据，恢复到初始状态
```

## 章节

```reasonix
mcp__novel-mcp-server__chapter_sync
  projectId: "<项目ID>"
  number: 1
  title: "标题"
  content: "# 正文..."
  phase: "draft"           // 可选
  characters: ["角色A","角色B"]   // 可选——自动建 ArcadeDB 出场关系
  items: ["圣剑"]                 // 可选
  locations: ["王都"]             // 可选——自动建 ArcadeDB 出场关系

mcp__novel-mcp-server__chapter_get
  projectId: "<项目ID>"
  number: 1

mcp__novel-mcp-server__chapter_list
  projectId: "<项目ID>"
  limit: 50       // 可选，默认50
  offset: 0       // 可选，默认0
```

## 人物

```reasonix
mcp__novel-mcp-server__character_save
  projectId: "<项目ID>"
  name: "角色名"
  identity: {"generation":1}   // 可选——区分同名角色（JSON，如尼尔双子）
  bio: "简介"
  traits: ["冷静","果断"]
  voiceSeeds: ["标志性台词1","台词2"]
  voiceMeta: {"禁止句式":["温柔地说"],"口癖":["哼"]}

mcp__novel-mcp-server__character_status
  projectId: "<项目ID>"
  name: "角色名"
  identity: {"generation":1}   // 可选——精确匹配；不传且同名多条时返回 allProfiles 列表
  // 返回：当前状态 + 全部历史快照 + 同名列表（如有）

mcp__novel-mcp-server__character_snapshot
  projectId: "<项目ID>"
  name: "角色名"
  chapterNumber: 1
  location: "地点"           // 可选——自动建 ArcadeDB [:VISITED] 边
  identity: {"generation":1}   // 可选——区分同名角色
  physical: "受伤"
  psychology: "愤怒"
  items: ["圣剑"]            // 可选
  summary: "本章结尾状态"

mcp__novel-mcp-server__character_snapshot_check
  projectId: "<项目ID>"
  modifiedChapter: 3
  // 返回：修改第3章会影响哪些后续快照
```

## 物品

```reasonix
mcp__novel-mcp-server__item_register
  projectId: "<项目ID>"
  name: "圣剑Excalibur"
  identity: {"era":"黄金时代"}   // 可选——区分同名物品
  itemType: "武器"
  description: "发光的圣剑"
  origin: "湖中仙女所赠"
  significance: "王权的象征"
  currentHolder: "亚瑟"
  currentLocation: "卡美洛"
  currentStatus: "正常"

mcp__novel-mcp-server__item_update
  projectId: "<项目ID>"
  name: "圣剑Excalibur"
  identity: {"era":"黄金时代"}   // 可选——区分同名物品
  chapter: 5
  event: "遗失"              // 赠予/遗失/发现/销毁
  newHolder: "莫德雷德"       // 可选
  newLocation: "战场"          // 可选
  newStatus: "遗失"
  changeDescription: "战斗中被击落"

mcp__novel-mcp-server__item_query
  projectId: "<项目ID>"
  name: "圣剑Excalibur"
  identity: {"era":"黄金时代"}   // 可选——不传且同名多条时返回 allProfiles
  // 返回：基本信息 + 归属变更历史 + ArcadeDB 关联图谱
```

## 地点

```reasonix
mcp__novel-mcp-server__location_register
  projectId: "<项目ID>"
  name: "卡美洛"
  identity: {"era":"黄金时代"}   // 可选——区分同名地点
  locationType: "建筑"          // 自然场景/建筑/聚落/室内
  region: "不列颠"
  firstChapter: 1
  canonDescription: "传说中的王城"
  actualAppearance: "叙事视角看到的实际样貌"
  sensoryDetail: "视觉/听觉/嗅觉/触觉描述"
  narrativeFunction: "王权中心"

mcp__novel-mcp-server__location_update
  projectId: "<项目ID>"
  name: "卡美洛"
  identity: {"era":"黄金时代"}   // 可选——区分同名地点
  chapter: 5
  triggerEvent: "战争"
  change: "城墙被毁"
  newStatus: "损毁"
  narrativeImpact: "王权动摇"

mcp__novel-mcp-server__location_status
  projectId: "<项目ID>"
  name: "卡美洛"
  identity: {"era":"黄金时代"}   // 可选——不传且同名多条时返回 allProfiles
  // 返回：初始信息 + 全部变更记录 + 当前状态
```

## 时间线

```reasonix
mcp__novel-mcp-server__timeline_create
  projectId: "<项目ID>"
  name: "主时间线"
  type: "main"          // main | flashback | branch | loop | alternative

mcp__novel-mcp-server__timeline_event_add
  projectId: "<项目ID>"
  timelineId: "<时间线ID>"
  name: "背叛场景"
  absoluteOrder: 5
  narrativeOrder: 3
  dateLabel: "星历3年"
  description: "关键时刻"
  status: "planned"          // planned | realized | modified | skipped
  criticality: "mandatory"   // mandatory | important | optional
  timeFlexibility: "flexible" // fixed | flexible | anytime
  plannedEventId: null       // 关联的计划事件ID

mcp__novel-mcp-server__timeline_event_update
  projectId: "<项目ID>"
  eventId: "<事件ID>"
  status: "modified"         // realized | modified | skipped
  chapterNumber: 6           // 实际发生在哪章
  actualDescription: "实际发生的情况"
  divergenceReason: "角色弧线调整"
  realizedByEventId: null    // 替代计划的实现事件ID

mcp__novel-mcp-server__timeline_check
  projectId: "<项目ID>"
  // 返回：顺序冲突 + mandatory 事件跳过/修改告警 + ArcadeDB 环检测

mcp__novel-mcp-server__timeline_link_create
  projectId: "<项目ID>"
  fromTimelineId: "..."
  toTimelineId: "..."
  linkType: "flashback_of"   // flashback_of | alternative_to | time_jump_from
  description: "回忆第3章的事件"

mcp__novel-mcp-server__timeline_link_query
  projectId: "<项目ID>"
  timelineId: "..."          // 可选——不传则查全部
  limit: 50                  // 可选，默认50
```

## 正典

```reasonix
mcp__novel-mcp-server__canon_import
  projectId: "<项目ID>"
  sourceName: "EVA Wiki"
  text: "绫波丽，EVA初号机驾驶员，14岁..."
  // 文本存入 canon_sources，字数确认

mcp__novel-mcp-server__canon_character_add
  projectId: "<项目ID>"
  name: "绫波丽"
  aliases: ["零","第一适格者"]
  bio: "EVA初号机驾驶员，14岁，沉默寡言"
  sourceId: "<canon_import 返回的 sourceId>"  // 可选

mcp__novel-mcp-server__canon_event_add
  projectId: "<项目ID>"
  name: "第6使徒战"
  timelinePos: "早期"
  dateLabel: "2015年"
  canonLevel: "核心"          // 核心 | 重要 | 次要
  description: "绫波丽首次出战"
  sourceId: "..."            // 可选

mcp__novel-mcp-server__canon_relationship_add
  projectId: "<项目ID>"
  fromChar: "绫波丽"
  toChar: "碇真嗣"
  relType: "同僚"
  note: "同为EVA驾驶员"
  sourceId: "..."            // 可选

mcp__novel-mcp-server__canon_search
  projectId: "<项目ID>"
  query: "绫波丽"
  embedding: "<bge-m3 向量>"  // 可选——精确语义搜索

mcp__novel-mcp-server__canon_verify
  projectId: "<项目ID>"
  entityId: "<条目UUID>"
  // 标记正典条目已人工审核

mcp__novel-mcp-server__canon_status_set
  projectId: "<项目ID>"
  canonEventId: "<正典事件ID>"
  status: "modified"          // pending | triggered | modified | skipped
  actualDescription: "实际发生了什么"
  occurredInChapter: 3
  divergenceReason: "为什么偏离正典"
```

## 宇宙

```reasonix
mcp__novel-mcp-server__universe_create
  projectId: "<项目ID>"
  name: "尼尔正史宇宙"
  type: "fanfic"          // original | fanfic | crossover
  description: "基于游戏原作的宇宙"

mcp__novel-mcp-server__universe_list
  projectId: "<项目ID>"

mcp__novel-mcp-server__universe_link
  projectId: "<项目ID>"
  fromUniverseId: "..."
  toUniverseId: "..."
  relationType: "parallel_to"  // parallel_to | derived_from | crosses_over_with
  description: "平行世界"
```

## 搜索 & 图谱

```reasonix
mcp__novel-mcp-server__fuzzy_search
  projectId: "<项目ID>"
  keyword: "关键词"
  limit: 10              // 可选

mcp__novel-mcp-server__rag_search
  projectId: "<项目ID>"
  query: "语义查询"
  embedding: "<bge-m3 向量>"
  k: 5                   // 可选
  synthesize: false

mcp__novel-mcp-server__semantic_search
  projectId: "<项目ID>"
  embedding: "<bge-m3 向量>"
  k: 5

mcp__novel-mcp-server__graph_query
  projectId: "<项目ID>"
  entityName: "绫波丽"
  depth: 2               // 可选，默认2，上限10

mcp__novel-mcp-server__graph_path
  projectId: "<项目ID>"
  from: "绫波丽"
  to: "碇真嗣"
```

## 推演

```reasonix
mcp__novel-mcp-server__deduce_behavior
  projectId: "<项目ID>"
  charNames: ["角色A","角色B"]
  scene: "场景描述"
  timeMode: "sequential"  // sequential | parallel | flashback | phase_shift

mcp__novel-mcp-server__deduce_outline
  projectId: "<项目ID>"
  mode: 1                 // 1=因果固定 2=因定果不定 3=无因无果
  premise: "前因"
  result: "结果"          // mode=1 必填
  characters: ["角色A"]

mcp__novel-mcp-server__deduce_verify
  projectId: "<项目ID>"
  deductionOutput: "{推演输出的JSON}"
```

## 伏笔

```reasonix
mcp__novel-mcp-server__register_foreshadowing
  projectId: "<项目ID>"
  code: "F001"
  description: "伏笔描述"
  fType: "🎯事件"          // 🔮情感 | 🎭身份 | 🎯事件 | 💡道具
  plantedChapter: 1
  characters: ["角色A"]    // 可选
```

## 语法检查

```reasonix
mcp__novel-mcp-server__grammar_check
  text: "他一个人走在街上，天气很好。"
  language: "zh-CN"       // 可选，默认 zh-CN
  // 返回：错误列表 + 修改建议 + 上下文片段
```

## 备份与恢复

```reasonix
mcp__novel-mcp-server__project_export
  projectId: "<项目ID>"
  // 返回：完整 JSON（章节/人物/时间线/正典/物品/地点/伏笔）

mcp__novel-mcp-server__project_import
  projectId: "<项目ID>"
  jsonData: "{...从 export 得到的完整 JSON ...}"
  // 自动重建 PG 数据 + ArcadeDB 图节点和关系
```
