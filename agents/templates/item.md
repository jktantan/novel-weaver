---
project: { project-slug }
name: { 物品名 }
item_type: { weapon/relic/token/tool/other }
current_holder: { 当前持有者 }
current_status: { 正常/损坏/遗失/销毁 }
---

# {物品名}

> 项目：{project-name}
> 类型：{weapon / relic / token / tool / other}

## 基本信息

| 项目    | 内容 |
|-------|----|
| 名称    |    |
| 类型    |    |
| 外观描述  |    |
| 用途/能力 |    |

## 来源

- 创造者/发现者：
- 首次出现章节：
- 来源事件：

## 归属历史

| 章节    | 持有者   | 事件       |
|-------|-------|----------|
| ch{N} | {角色A} | 首次出现     |
| ch{N} | {角色B} | 赠予/丢失/抢夺 |

## 剧情意义

- 在主线中的作用：
- 与其他物品/角色的关联：

## 备注

- MCP 工具：`item_register` 注册 → `item_update` 更新状态/持有者 → `item_query` 查询详情和关联图谱
- ArcadeDB 关系：`(:Character)-[:OWNS]->(:Item)` / `(:Location)-[:CONTAINS]->(:Item)` /
  `(:Item)-[:APPEARS_IN]->(:Chapter)`
