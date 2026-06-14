# Novel Weaver — 项目目录结构规范 / プロジェクトディレクトリ構成仕様 / Project Directory Structure Specification

> **CN** 版本 v1.0 · 适用于：同人小说 + 原创小说 · 原则：三层结构——共享方法层 / 项目层 / 项目内按创作阶段分层  
> **JP** バージョン v1.0 · 対象：二次創作＋オリジナル小説 · 原則：三層構造——共有メソッド層 / プロジェクト層 / プロジェクト内の創作段階別階層  
> **EN** Version v1.0 · For: fanfic + original fiction · Principle: three-layer structure — shared methods / project layer / per-project phase layer

---

## 中文

### 一、顶层结构

```
~/novel-vault/
├── _shared/                  ← 跨项目共享（写作方法论、模板、检查清单）
├── {project-slug}/           ← 每个项目一个目录（如 以你的名、星辰之海）
├── _archive/                 ← 归档项目（不再活跃）
└── README.md                 ← vault 总说明
```

---

### 二、共享层 `_shared/`

跨项目通用的写作方法、Prompt 模板、检查清单。改一次，所有项目收益。

```
_shared/
├── tone/                     ← 写作风格指南
│   ├── 写作指纹-默认.md       # 默认写作指纹（可被项目级覆盖）
│   ├── 叙事基调指南.md
│   └── 反AI写作检查清单.md
│
├── prompts/                  ← 章节写作 Prompt（按小说类型分）
│   ├── 写作prompt-原创.md
│   └── 写作prompt-同人.md
│
├── checklists/               ← 写作前后检查清单
│   └── 矛盾检查清单.md
│
└── templates/                ← 新建项目用的脚手架
    ├── project-meta.yaml     # 项目元数据模板
    ├── character-profile.md  # 人物画像模板
    ├── chapter-outline.md    # 章节大纲模板
    ├── chapter-draft.md      # 章节正文模板（含 front matter）
    └── project.ai.md         # Reasonix 项目操作手册模板
```

#### 文件说明

| 文件 | 说明 |
|------|------|
| `写作指纹-默认.md` | 默认写作风格——白描、分段节奏、对话密度。项目级 `project.ai.md` 可以覆盖 |
| `叙事基调指南.md` | 声线规范、沉默分类、情感表达层级 |
| `反AI写作检查清单.md` | AI 文本特征禁令 + 自检流程 |
| `写作prompt-原创.md` | 原创小说写作 Prompt——不含正典约束段 |
| `写作prompt-同人.md` | 同人小说写作 Prompt——含正典红线约束段 |
| `矛盾检查清单.md` | 写作前后硬性检查项 |
| `project-meta.yaml` | 项目元数据模板——创建新项目时复制到项目根目录 |
| `character-profile.md` | 人物画像模板——Bio/Traits/Voice/Relationships 固定结构 |
| `chapter-outline.md` | 章节大纲模板——含 front matter |
| `chapter-draft.md` | 章节正文模板——含 front matter |
| `project.ai.md` | Reasonix 操作手册模板——写作约束、每章后流程 |

---

### 三、项目层 `{project-slug}/`

每个小说项目一个目录。项目名用英文 slug（`yideiming`）或中文名（`以你的名`）均可。

#### 必选文件

| 文件 | 说明 |
|------|------|
| `project.yaml` | 🔴 机器可读的项目元数据 |
| `project.ai.md` | Reasonix/Claude 项目操作手册 |

#### 可选目录（按需存在）

| 目录                   | 何时存在    | 说明                          |
|----------------------|---------|-----------------------------|
| `canon/`             | 同人小说    | 正典资料——外部来源，不可修改的事实          |
| `worldbuilding/`     | 同人 + 原创 | 本故事的世界观扩展（同人=非正典设定，原创=全部设定） |
| `characters/`        | 总是      | 本故事人物画像——每个角色一个 `.md`       |
| `states/`            | 总是      | 人物状态追踪（每章后更新）               |
| `foreshadowing.yaml` | 总是      | 伏笔登记                        |
| `timeline/`          | 可选      | 故事时间线                       |
| `outlines/`          | 总是      | 章节大纲（计划）                    |
| `drafts/`            | 总是      | 章节正文（成品）                    |
| `items/`             | 可选      | 故事关键物品档案——每件物品一个 `.md`      |

#### 3.1 `project.yaml` —— 项目元数据

```yaml
# project.yaml — 机器可读的项目元数据
# Reasonix 和 MCP 工具通过读取此文件了解项目基本信息

name: "以你的名"              # 项目中文名
slug: "yideiming"            # 项目英文 slug（用于 API 路径）
type: fanfic                  # original | fanfic
status: active                # active | archived | draft

# ── 同人小说专有字段 ──
fandom: "江湖风云录"
canon_version: "三部曲 + 外传"
canon_sources:
  - name: "江湖风云录·第一部"
    type: game
    year: 2021
  - name: "江湖风云录·外传"
    type: game
    year: 2017
canon_constraints:            # 正典红线——推演不能违反
  - "所有正典事件结果不可修改"
  - "正典人物死亡时间不可提前或延后"
  - "正典人物核心性格不可反转"

# ── 原创小说专有字段 ──
# world_type: "玄幻"          # 原创小说填世界观类型
# magic_system: "元素体系"    # 如果有魔法/力量体系
# era: "架空古代"             # 时代背景

# ── 通用字段 ──
protagonist: 古月              # 主角名
pov: "古月第三人称有限"        # 叙事视角
language: zh-CN                # 写作语言
phases:                        # 故事阶段
  - name: 降临期
    chapters: [1, 2]
  - name: 旁观期
    chapters: [3, 4, 5]
  - name: 石化期
    chapters: [6, 7, 8, 9, 10]
  - name: 回归期
    chapters: [11, 12, 13, 14]
  - name: 之后
    chapters: [15]

writing_constraints:           # 写作硬约束
  chapter_word_count:
    min: 5000
    max: 9000
  forbidden:
    - "轻小说翻译腔（'呐''嘛''呢'）"
    - "副词修饰情感（'温柔地说'）"
    - "'不是…而是…'句式"
  dialogue_format:
    speech: '""'               # 对话用中文双引号
    thought: "『』"             # 内心用角括号
  style_fingerprint: "远瞳《异常生物见闻录》"
```

#### 3.2 `project.ai.md` —— Reasonix 操作手册

每个项目的 AI 操作手册——Reasonix 写作前必读。结构：

```markdown
# project.ai.md — {项目中文名}

> 类型：{原创/同人}
> 主角：{主角名}
> 叙事视角：{POV 描述}

## 一、写作约束
（章节字数、语言风格、对话格式、视角规则）

## 二、写作前必读文件
（按优先顺序列出）

## 三、每章写作后流程
（更新状态、登记伏笔、审校、反向核对画像）

## 四、人物速查
（主要角色 + 存活状态 + 声线要点）

## 五、阶段速查
（各阶段的核心任务和正典约束）
```

**注意**：`project.ai.md` 不要放 `project.yaml` 已经有的信息（如阶段划分、字数限制）。两者互补——`project.yaml` 是机器读的结构化元数据，`project.ai.md` 是 AI 读的写作操作手册。

#### 3.3 `canon/` —— 同人小说正典资料

```
canon/
├── timeline.md               # 正典事件时间线（🔴🟡🟢 约束标注）
├── world-overview.md         # 正典世界观概述
├── characters/               # 正典人物事实（每人一个文件）
│   ├── 林青.md               #   - 正典年龄、经历、关键事件
│   ├── 原作资料.md               #   - 不包含"本故事中"的演绎
│   └── ...
├── relationships.yaml        # 正典人物关系（机器可读）
│   # 例:
│   # - from: {角色A}
│   #   to: {角色B}
│   #   type: {关系类型}
│   #   canon_level: 🔴
│   #   note: {描述}
└── sources.yaml              # 资料出处
    # - name: "{作品名}"
    #   type: {game/book}
    #   url: ...
```

#### 3.4 `worldbuilding/` —— 世界观扩展

| 同人小说 | 原创小说 |
|----------|---------|
| 非正典但本故事有的设定（地下掩体、基因修复方案） | 全部世界观（魔法体系、种族势力、历史年表、地理志） |

```
worldbuilding/
├── 世界观总览.md              # 核心规则 / 魔法体系
├── 历史年表.md               # 故事内历史
├── 地理志.md                 # 世界地图 / 地点
├── 种族与势力.md             # 各方势力
└── ...                       # 故事特有设定
```

#### 3.5 `characters/` —— 本故事人物画像

```
characters/
├── {主角}.md                    # 主角画像——含演进记录
├── {重要角色}.md                # 每章后如人物走向与预期不同→追加 "正文补充（第XXXX章后）"
├── {角色C}.md
└── ...
```

**人物画像模板**（从 `_shared/templates/character-profile.md` 复制）：

```markdown
# {角色名}

> 项目：{project-name}
> 类型：{major / minor / extra}

## 基本信息
| 项目 | 内容 |
|------|------|
| 姓名 | |
| 年龄 | |
| 性别 | |
| 所属 | |

## 性格特征
- 

## 声线
- 口吻：
- 句式：
- 禁忌：
- 种子台词：
  - ""
  - ""

## 与其他人物关系
| 人物 | 关系类型 | 信任度(0-10) | 备注 |
|------|---------|-------------|------|
| | | | |

## 剧情弧光
（角色从开始到结束的变化轨迹）

## 正文演进记录
（每章后如角色走向与画像预期不同，在此追加记录）
```

#### 3.6 `states/` —— 人物状态追踪

```
states/
├── current.yaml               # 最新状态（机器可读，每章更新）
└── history/                   # 历史快照（可选——MCP 工具自动写入）
    ├── ch0001.yaml
    └── ...
```

**`current.yaml` 格式**：

```yaml
# states/current.yaml — 人物当前状态
updated_at: "2026-06-09"
last_chapter: 6

characters:
  {主角}:
    location: "{当前地点}"
    physical: "{生理状态}"
    psychology: "{心理状态}"
    items: ["{物品1}", "{物品2}"]
    summary: "{本章角色动态概述}"

  {重要角色}:
    location: "{当前地点}"
    physical: "{生理状态}"
    psychology: "{心理状态}"
    items: ["{物品1}"]
    summary: "{本章角色动态概述}"
```

#### 3.7 `foreshadowing.yaml` —— 伏笔登记

```yaml
# foreshadowing.yaml — 伏笔管理系统
foreshadows:
  - code: F001
    type: "🔮"
    description: "{伏笔描述}"
    planted: {N}
    payoff: null
    status: "🌱"
    characters: ["{角色A}", "{角色B}"]
    
  - code: F002
    type: "🎭"
    description: "{伏笔描述}"
    planted: {N}
    payoff: null
    status: "🌱"
    characters: ["{角色A}"]
```

#### 3.8 `outlines/` —— 章节大纲

```
outlines/
├── 总纲.md                    # 故事核心、阶段结构、结局愿景
├── ch0001-降临.md             # 每章大纲
├── ch0002-这还是国内吗（上）.md
└── ...
```

**大纲模板**（`ch{NNNN}-{标题}.md`）：

```markdown
---
project: {project-slug}
type: outline
chapter: {number}
title: {标题}
phase: {阶段名}
goal: "本章目标——一句话"
characters: [出场角色]
---

# 第{NNNN}章 {标题}

## 场景一：{场景名}
（简要描述——环境、人物、发生了什么）

## 场景二：{场景名}
...

## 场景三：{场景名}
...

---

## 关联文件
- 大纲/总纲.md
- characters/xxx.md
- canon/xxx.md（同人）
```

#### 3.9 `drafts/` —— 章节正文

```
drafts/
├── ch0001-降临.md
├── ch0002-这还是国内吗（上）.md
└── ...
```

**正文模板**（`ch{NNNN}-{标题}.md`）：

```markdown
---
project: {project-slug}
type: draft
chapter: {number}
title: {标题}
phase: {阶段名}
characters: [出场角色列表]
location: {主要地点}
word_count: {写完填写}
status: draft                # draft → review → final
---

# 第{NNNN}章 {标题}

（正文，5000-9000 字）
```

---

#### 3.10 `items/` —— 物品档案

```
items/
├── 圣剑·誓约.md              # 每件物品一个文件
├── 青铜怀表.md
└── ...
```

**物品档案模板**：

```markdown
---
project: {project-slug}
name: {物品名}
item_type: {weapon/relic/token/tool/other}
current_holder: {当前持有者}
current_status: {正常/损坏/遗失/销毁}
---

# {物品名}

## 基本信息
| 项目 | 内容 |
|------|------|
| 名称 | |
| 类型 | |
| 外观描述 | |
| 用途/能力 | |

## 来源
- 创造者/发现者：
- 首次出现章节：
- 来源事件：

## 归属历史
| 章节 | 持有者 | 事件 |
|------|--------|------|
| ch{N} | {角色A} | 首次出现 |
| ch{N} | {角色B} | 赠予/丢失/抢夺 |
```

> **注意**：物品档案为可选目录。`states/current.yaml` 中的 `items` 字符串数组记录角色当前持有物品名——物品名应尽量与
`items/` 目录下的文件名一致，以便交叉引用。

---

### 四、命名规范

| 对象    | 格式                   | 示例                         |
|-------|----------------------|----------------------------|
| 项目目录  | `{中文名}` 或 `{英文slug}` | `{项目名}` / `{project-slug}` |
| 大纲文件  | `ch{NNNN}-{标题}.md`   | `ch0005-第一个夜晚.md`          |
| 正文文件  | `ch{NNNN}-{标题}.md`   | `ch0005-第一个夜晚.md`          |
| 人物画像  | `{角色名}.md`           | `林青.md`                    |
| 物品档案  | `{物品名}.md`           | `圣剑·誓约.md`                 |
| 正典人物  | `{角色名}.md`           | `林青.md`                    |
| 状态快照  | `ch{NNNN}.yaml`      | `ch0006.yaml`              |
| 伏笔编号  | `F{NNN}`             | `F001`                     |
| 世界观文件 | `{主题}.md`            | `魔法体系.md`                  |

**注意**：大纲和正文使用相同文件名格式，但**分属不同目录**（`outlines/` vs `drafts/`）——不会混淆。

---

### 五、同人 vs 原创的文件清单对比

```
项目根目录:

同人小说（{project-name}）               原创小说（{project-name}）
├── project.yaml                    ├── project.yaml
├── project.ai.md                   ├── project.ai.md
├── canon/          ← 同人专属     │
│   ├── timeline.md                │
│   ├── world-overview.md          │
│   ├── characters/                │
│   ├── relationships.yaml         │
│   └── sources.yaml               │
├── worldbuilding/ ← 非正典扩展    ├── worldbuilding/ ← 全部世界观
│   ├── {设定文件1}.md            │   ├── 世界观总览.md
│   ├── {设定文件2}.md          │   ├── 魔法体系.md
│   ├── {设定文件3}.md        │   ├── 种族与势力.md
│   └── {设定文件4}.md              │   ├── 历史年表.md
│                                   │   └── 地理志.md
├── characters/                     ├── characters/
├── states/                         ├── states/
├── foreshadowing.yaml              ├── foreshadowing.yaml
├── timeline/                       ├── timeline/
├── outlines/                       ├── outlines/
├── items/     ← 可选              ├── items/   ← 可选
└── drafts/                         └── drafts/
```

---

### 六、创建新项目的 Checklist

1. [ ] 在 `novel-vault/` 下创建项目目录
2. [ ] 从 `_shared/templates/` 复制 `project-meta.yaml` → 重命名为 `project.yaml` → 填写
3. [ ] 从 `_shared/templates/` 复制 `project.ai.md` → 填写写作约束
4. [ ] 创建 `outlines/`、`drafts/`、`characters/`、`states/` 目录
5. [ ] 如果是同人：创建 `canon/` 目录 → 导入正典资料
6. [ ] 如果是原创：创建 `worldbuilding/` 目录 → 开始世界观构建
7. [ ] 创建 `foreshadowing.yaml`（空数组）
8. [ ] 创建 `states/current.yaml`（空）
9. [ ] 定义主要角色 → 每人一个 `.md` 写入 `characters/`
10. [ ] 如有重要物品：创建 `items/` 目录 → 每件物品一个 `.md` 写入 `items/`
11. [ ] 调用 MCP `project_init` 工具 → 服务端创建项目记录
12. [ ] 开始写 `总纲.md` → 开始写作

---

## 日本語

### 一、トップレベル構成

```
~/novel-vault/
├── _shared/                  ← プロジェクト横断共有（執筆方法論、テンプレート、チェックリスト）
├── {project-slug}/           ← プロジェクトごとに一つのディレクトリ（例：以你的名、星辰之海）
├── _archive/                 ← アーカイブ済みプロジェクト（非アクティブ）
└── README.md                 ← vault 全体の説明
```

---

### 二、共有層 `_shared/`

プロジェクト横断で共通利用する執筆方法、Prompt テンプレート、チェックリスト。一度改めれば全プロジェクトに反映。

```
_shared/
├── tone/                     ← 執筆スタイルガイド
│   ├── 写作指纹-默认.md       # デフォルト執筆指針（プロジェクトレベルで上書き可）
│   ├── 叙事基调指南.md
│   └── 反AI写作检查清单.md
│
├── prompts/                  ← チャプター執筆用 Prompt（小説タイプ別）
│   ├── 写作prompt-原创.md
│   └── 写作prompt-同人.md
│
├── checklists/               ← 執筆前後チェックリスト
│   └── 矛盾检查清单.md
│
└── templates/                ← 新規プロジェクト用スキャフォールド
    ├── project-meta.yaml     # プロジェクトメタデータテンプレート
    ├── character-profile.md  # キャラクター設定テンプレート
    ├── chapter-outline.md    # チャプター概要テンプレート
    ├── chapter-draft.md      # チャプター本文テンプレート（front matter 付き）
    └── project.ai.md         # Reasonix 操作マニュアルテンプレート
```

#### ファイル説明

| ファイル | 説明 |
|----------|------|
| `写作指纹-默认.md` | デフォルト執筆スタイル——白描、段落リズム、会話密度。プロジェクトレベルの `project.ai.md` で上書き可 |
| `叙事基调指南.md` | 声線規範、沈黙分類、感情表現階層 |
| `反AI写作检查清单.md` | AI テキスト特徴禁令＋自己チェック手順 |
| `写作prompt-原创.md` | オリジナル小説用 Prompt——正典制約なし |
| `写作prompt-同人.md` | 二次創作小説用 Prompt——正典制約あり |
| `矛盾检查清单.md` | 執筆前後ハードチェック項目 |
| `project-meta.yaml` | プロジェクトメタデータテンプレート——新規作成時にプロジェクトルートにコピー |
| `character-profile.md` | キャラクター設定テンプレート——Bio/Traits/Voice/Relationships 固定構造 |
| `chapter-outline.md` | チャプター概要テンプレート——front matter 付き |
| `chapter-draft.md` | チャプター本文テンプレート——front matter 付き |
| `project.ai.md` | Reasonix 操作マニュアルテンプレート——執筆制約、章ごとのフロー |

---

### 三、プロジェクト層 `{project-slug}/`

小説プロジェクトごとに一つのディレクトリ。プロジェクト名は英字 slug（`yideiming`）または中国語名（`以你的名`）のどちらでも可。

#### 必須ファイル

| ファイル | 説明 |
|----------|------|
| `project.yaml` | 🔴 機械可読なプロジェクトメタデータ |
| `project.ai.md` | Reasonix/Claude プロジェクト操作マニュアル |

#### オプションディレクトリ（必要に応じて存在）

| ディレクトリ               | 存在条件       | 説明                             |
|----------------------|------------|--------------------------------|
| `canon/`             | 二次創作       | 正典資料——外部ソース、変更不可の事実            |
| `worldbuilding/`     | 二次創作＋オリジナル | 本作の世界観拡張（二次創作＝非正典設定、オリジナル＝全設定） |
| `characters/`        | 常に         | 本作のキャラクター設定——各キャラクター一つの `.md`  |
| `states/`            | 常に         | キャラクター状態追跡（章ごとに更新）             |
| `foreshadowing.yaml` | 常に         | 伏線管理                           |
| `timeline/`          | オプション      | ストーリー時系列                       |
| `outlines/`          | 常に         | チャプター概要（計画）                    |
| `drafts/`            | 常に         | チャプター本文（成果物）                   |
| `items/`             | オプション      | 物語の重要アイテムアーカイブ——各アイテム1つの `.md` |

#### 3.1 `project.yaml` —— プロジェクトメタデータ

```yaml
# project.yaml — 機械可読なプロジェクトメタデータ
# Reasonix と MCP ツールがこのファイルを読み、プロジェクト基本情報を取得

name: "以你的名"              # プロジェクト中国語名
slug: "yideiming"            # プロジェクト英字 slug（API パス用）
type: fanfic                  # original | fanfic
status: active                # active | archived | draft

# ── 二次創作専用フィールド ──
fandom: "江湖风云录"
canon_version: "三部曲 + 外传"
canon_sources:
  - name: "江湖风云录·第一部"
    type: game
    year: 2021
  - name: "江湖风云录·外传"
    type: game
    year: 2017
canon_constraints:            # 正典の制約——推論で違反不可
  - "すべての正典イベントの結果は変更不可"
  - "正典キャラの死亡タイミングは変更不可"
  - "正典キャラの核となる性格は反転不可"

# ── オリジナル小説専用フィールド ──
# world_type: "玄幻"
# magic_system: "元素体系"
# era: "架空古代"

# ── 共通フィールド ──
protagonist: 古月              # 主人公名
pov: "古月第三人称有限"        # 叙述視点
language: zh-CN                # 執筆言語
phases:                        # ストーリー段階
  - name: 降临期
    chapters: [1, 2]
  - name: 旁观期
    chapters: [3, 4, 5]
  - name: 石化期
    chapters: [6, 7, 8, 9, 10]
  - name: 回归期
    chapters: [11, 12, 13, 14]
  - name: 之后
    chapters: [15]

writing_constraints:           # 執筆ハード制約
  chapter_word_count:
    min: 5000
    max: 9000
  forbidden:
    - "ライトノベル翻訳調（'呐''嘛''呢'）"
    - "副詞による感情修飾（'優しく言う'）"
    - "'不是…而是…'構文"
  dialogue_format:
    speech: '""'               # 会話は中国語二重引用符
    thought: "『』"             # 内心は角括弧
  style_fingerprint: "远瞳《异常生物见闻录》"
```

#### 3.2 `project.ai.md` —— Reasonix 操作マニュアル

各プロジェクトの AI 操作マニュアル——Reasonix 執筆前に必読。構成：

```markdown
# project.ai.md — {プロジェクト中国語名}

> タイプ：{オリジナル/二次創作}
> 主人公：{主人公名}
> 叙述視点：{POV 説明}

## 一、執筆制約
（章の文字数、言語スタイル、会話フォーマット、視点ルール）

## 二、執筆前必読ファイル
（優先順に列挙）

## 三、各章執筆後のフロー
（状態更新、伏線登録、校正、設定の逆方向確認）

## 四、キャラクタークイックリファレンス
（主要キャラ＋生死状態＋声線ポイント）

## 五、フェーズクイックリファレンス
（各フェーズの核心タスクと正典制約）
```

**注意**：`project.ai.md` には `project.yaml` に既にある情報（フェーズ区分や文字数制限など）は入れない。両者は補完関係——`project.yaml` は機械が読む構造化メタデータ、`project.ai.md` は AI が読む執筆操作マニュアル。

#### 3.3 `canon/` —— 二次創作の正典資料

```
canon/
├── timeline.md               # 正典イベント時系列（🔴🟡🟢 制約ラベル）
├── world-overview.md         # 正典世界観概要
├── characters/               # 正典キャラクター事実（一人一ファイル）
│   ├── 林青.md               #   - 正典の年齢、経歴、キーイベント
│   ├── 原作资料.md               #   - 「本作における」解釈は含まない
│   └── ...
├── relationships.yaml        # 正典キャラクター関係（機械可読）
│   # 例:
│   # - from: {キャラA}
│   #   to: {キャラB}
│   #   type: {関係タイプ}
│   #   canon_level: 🔴
│   #   note: {説明}
└── sources.yaml              # 資料出典
    # - name: "{作品名}"
    #   type: {game/book}
    #   url: ...
```

#### 3.4 `worldbuilding/` —— 世界観拡張

| 二次創作 | オリジナル小説 |
|----------|---------------|
| 非正典だが本作にある設定（地下シェルター、遺伝子修復案） | 全世界観（魔法体系、種族勢力、歴史年表、地理誌） |

```
worldbuilding/
├── 世界观总览.md              # 核心ルール / 魔法体系
├── 历史年表.md               # 作中歴史
├── 地理志.md                 # 世界地図 / ロケーション
├── 种族与势力.md             # 各方勢力
└── ...                       # 作品固有設定
```

#### 3.5 `characters/` —— 本作のキャラクター設定

```
characters/
├── {主人公}.md                  # 主人公設定——進化記録含む
├── {重要キャラ}.md              # 章ごとに想定と異なる展開→「正文补充（第XXXX章后）」を追記
├── {キャラC}.md
└── ...
```

**キャラクター設定テンプレート**（`_shared/templates/character-profile.md` からコピー）：

```markdown
# {キャラクター名}

> プロジェクト：{project-name}
> タイプ：{major / minor / extra}

## 基本情報
| 項目 | 内容 |
|------|------|
| 名前 | |
| 年齢 | |
| 性別 | |
| 所属 | |

## 性格特徴
- 

## 声線
- 口調：
- 文型：
- 禁忌：
- シード台詞：
  - ""
  - ""

## 他キャラとの関係
| キャラ | 関係タイプ | 信頼度(0-10) | 備考 |
|--------|-----------|-------------|------|
| | | | |

## ストーリーアーク
（キャラクターの開始から終了までの変化軌跡）

## 本文進化記録
（章ごとに想定と異なる展開になった場合、ここに追記）
```

#### 3.6 `states/` —— キャラクター状態追跡

```
states/
├── current.yaml               # 最新状態（機械可読、章ごとに更新）
└── history/                   # 履歴スナップショット（オプション——MCP ツールが自動書き込み）
    ├── ch0001.yaml
    └── ...
```

**`current.yaml` フォーマット**：

```yaml
# states/current.yaml — キャラクター現在状態
updated_at: "2026-06-09"
last_chapter: 6

characters:
  {主人公}:
    location: "{現在地}"
    physical: "{生理状態}"
    psychology: "{心理状態}"
    items: ["{アイテム1}", "{アイテム2}"]
    summary: "{本章のキャラクター動向概要}"

  {重要キャラ}:
    location: "{現在地}"
    physical: "{生理状態}"
    psychology: "{心理状態}"
    items: ["{アイテム1}"]
    summary: "{本章のキャラクター動向概要}"
```

#### 3.7 `foreshadowing.yaml` —— 伏線管理

```yaml
# foreshadowing.yaml — 伏線管理システム
foreshadows:
  - code: F001
    type: "🔮"
    description: "{伏線説明}"
    planted: {N}
    payoff: null
    status: "🌱"
    characters: ["{キャラA}", "{キャラB}"]
    
  - code: F002
    type: "🎭"
    description: "{伏線説明}"
    planted: {N}
    payoff: null
    status: "🌱"
    characters: ["{キャラA}"]
```

#### 3.8 `outlines/` —— チャプター概要

```
outlines/
├── 总纲.md                    # ストーリー核心、フェーズ構造、結末ビジョン
├── ch0001-降临.md             # 各章の概要
├── ch0002-这还是国内吗（上）.md
└── ...
```

**概要テンプレート**（`ch{NNNN}-{タイトル}.md`）：

```markdown
---
project: {project-slug}
type: outline
chapter: {number}
title: {タイトル}
phase: {フェーズ名}
goal: "本章の目標——一言"
characters: [登場キャラ]
---

# 第{NNNN}章 {タイトル}

## シーン一：{シーン名}
（簡潔な説明——環境、キャラ、何が起きたか）

## シーン二：{シーン名}
...

## シーン三：{シーン名}
...

---

## 関連ファイル
- 大纲/总纲.md
- characters/xxx.md
- canon/xxx.md（二次創作）
```

#### 3.9 `drafts/` —— チャプター本文

```
drafts/
├── ch0001-降临.md
├── ch0002-这还是国内吗（上）.md
└── ...
```

**本文テンプレート**（`ch{NNNN}-{タイトル}.md`）：

```markdown
---
project: {project-slug}
type: draft
chapter: {number}
title: {タイトル}
phase: {フェーズ名}
characters: [登場キャラリスト]
location: {主要ロケーション}
word_count: {執筆後記入}
status: draft                # draft → review → final
---

# 第{NNNN}章 {タイトル}

（本文、5000-9000 字）
```

---

#### 3.10 `items/` —— アイテムアーカイブ

```
items/
├── 圣剑·誓约.md              # 各アイテム1つのファイル
├── 青铜怀表.md
└── ...
```

**アイテムアーカイブテンプレート**：

```markdown
---
project: {project-slug}
name: {アイテム名}
item_type: {weapon/relic/token/tool/other}
current_holder: {現在の所持者}
current_status: {正常/破損/紛失/破壊}
---

# {アイテム名}

## 基本情報
| 項目 | 内容 |
|------|------|
| 名称 | |
| タイプ | |
| 外観説明 | |
| 用途/能力 | |

## 来歴
- 創造者/発見者：
- 初登場章：
- 来歴イベント：

## 所有履歴
| 章 | 所持者 | イベント |
|------|--------|------|
| ch{N} | {キャラA} | 初登場 |
| ch{N} | {キャラB} | 贈与/紛失/強奪 |
```

> **注意**：アイテムアーカイブはオプション。`states/current.yaml` の `items` 文字列配列はキャラクターの現在所持アイテム名を記録——アイテム名は
`items/` ディレクトリのファイル名と一致させることで相互参照可能。

---

### 四、命名規則

| 対象           | フォーマット                  | 例                          |
|--------------|-------------------------|----------------------------|
| プロジェクトディレクトリ | `{中国語名}` または `{英字slug}` | `{项目名}` / `{project-slug}` |
| 概要ファイル       | `ch{NNNN}-{タイトル}.md`    | `ch0005-第一个夜晚.md`          |
| 本文ファイル       | `ch{NNNN}-{タイトル}.md`    | `ch0005-第一个夜晚.md`          |
| キャラクター設定     | `{キャラ名}.md`             | `林青.md`                    |
| アイテムアーカイブ    | `{アイテム名}.md`            | `圣剑·誓约.md`                 |
| 正典キャラ        | `{キャラ名}.md`             | `林青.md`                    |
| 状態スナップショット   | `ch{NNNN}.yaml`         | `ch0006.yaml`              |
| 伏線番号         | `F{NNN}`                | `F001`                     |
| 世界観ファイル      | `{テーマ}.md`              | `魔法体系.md`                  |

**注意**：概要と本文は同じファイル名フォーマットだが、**別ディレクトリ**（`outlines/` vs `drafts/`）に属するため混同しない。

---

### 五、二次創作 vs オリジナルのファイル一覧比較

```
プロジェクトルート:

二次創作（{project-name}）                 オリジナル小説（{project-name}）
├── project.yaml                    ├── project.yaml
├── project.ai.md                   ├── project.ai.md
├── canon/          ← 二次創作専用 │
│   ├── timeline.md                │
│   ├── world-overview.md          │
│   ├── characters/                │
│   ├── relationships.yaml         │
│   └── sources.yaml               │
├── worldbuilding/ ← 非正典拡張    ├── worldbuilding/ ← 全世界観
│   ├── {設定ファイル1}.md         │   ├── 世界观总览.md
│   ├── {設定ファイル2}.md         │   ├── 魔法体系.md
│   ├── {設定ファイル3}.md         │   ├── 种族与势力.md
│   └── {設定ファイル4}.md         │   ├── 历史年表.md
│                                   │   └── 地理志.md
├── characters/                     ├── characters/
├── states/                         ├── states/
├── foreshadowing.yaml              ├── foreshadowing.yaml
├── timeline/                       ├── timeline/
├── outlines/                       ├── outlines/
├── items/     ← オプション         ├── items/   ← オプション
└── drafts/                         └── drafts/
```

---

### 六、新規プロジェクト作成の Checklist

1. [ ] `novel-vault/` 下にプロジェクトディレクトリを作成
2. [ ] `_shared/templates/` から `project-meta.yaml` をコピー→ `project.yaml` にリネーム→記入
3. [ ] `_shared/templates/` から `project.ai.md` をコピー→執筆制約を記入
4. [ ] `outlines/`、`drafts/`、`characters/`、`states/` ディレクトリを作成
5. [ ] 二次創作の場合：`canon/` ディレクトリを作成→正典資料をインポート
6. [ ] オリジナルの場合：`worldbuilding/` ディレクトリを作成→世界観構築を開始
7. [ ] `foreshadowing.yaml`（空配列）を作成
8. [ ] `states/current.yaml`（空）を作成
9. [ ] 主要キャラクターを定義→各 `.md` を `characters/` に書き込み
10. [ ] 重要アイテムがある場合：`items/` ディレクトリを作成→各アイテム1つの `.md` を `items/` に書き込み
11. [ ] MCP `project_init` ツールを呼び出し→サーバー側にプロジェクトレコード作成
12. [ ] `总纲.md` を書き始める→執筆開始

---

## English

### I. Top-Level Structure

```
~/novel-vault/
├── _shared/                  ← Cross-project shared resources (writing methodology, templates, checklists)
├── {project-slug}/           ← One directory per project (e.g., 以你的名, 星辰之海)
├── _archive/                 ← Archived projects (no longer active)
└── README.md                 ← Vault overview
```

---

### II. Shared Layer `_shared/`

Writing methods, Prompt templates, and checklists shared across projects. Change once, benefit everywhere.

```
_shared/
├── tone/                     ← Writing style guide
│   ├── 写作指纹-默认.md       # Default fingerprint (overridable at project level)
│   ├── 叙事基调指南.md
│   └── 反AI写作检查清单.md
│
├── prompts/                  ← Chapter writing prompts (by fiction type)
│   ├── 写作prompt-原创.md
│   └── 写作prompt-同人.md
│
├── checklists/               ← Pre/post writing checklists
│   └── 矛盾检查清单.md
│
└── templates/                ← Scaffolds for new projects
    ├── project-meta.yaml     # Project metadata template
    ├── character-profile.md  # Character profile template
    ├── chapter-outline.md    # Chapter outline template (with front matter)
    ├── chapter-draft.md      # Chapter draft template (with front matter)
    └── project.ai.md         # Reasonix operation manual template
```

#### File Descriptions

| File | Description |
|------|-------------|
| `写作指纹-默认.md` | Default writing fingerprint — plain description, paragraph rhythm, dialogue density. Overridable via project-level `project.ai.md` |
| `叙事基调指南.md` | Voice specs, silence classification, emotional expression hierarchy |
| `反AI写作检查清单.md` | AI text feature bans + self-check procedure |
| `写作prompt-原创.md` | Original fiction writing prompt — no canon constraints |
| `写作prompt-同人.md` | Fanfic writing prompt — includes canon red-line constraints |
| `矛盾检查清单.md` | Hard checklist items for pre/post writing |
| `project-meta.yaml` | Project metadata template — copy to project root when creating |
| `character-profile.md` | Character profile template — fixed structure: Bio/Traits/Voice/Relationships |
| `chapter-outline.md` | Chapter outline template — with front matter |
| `chapter-draft.md` | Chapter draft template — with front matter |
| `project.ai.md` | Reasonix operation manual template — writing constraints, post-chapter workflow |

---

### III. Project Layer `{project-slug}/`

One directory per novel project. The project name can be an English slug (`yideiming`) or Chinese name (`以你的名`).

#### Required Files

| File | Description |
|------|-------------|
| `project.yaml` | 🔴 Machine-readable project metadata |
| `project.ai.md` | Reasonix/Claude project operation manual |

#### Optional Directories

| Directory            | When Present      | Description                                                            |
|----------------------|-------------------|------------------------------------------------------------------------|
| `canon/`             | Fanfic            | Canon materials — external sources, immutable facts                    |
| `worldbuilding/`     | Fanfic + Original | Worldbuilding extensions (fanfic = non-canon, original = all settings) |
| `characters/`        | Always            | Character profiles — one `.md` per character                           |
| `states/`            | Always            | Character state tracking (updated per chapter)                         |
| `foreshadowing.yaml` | Always            | Foreshadowing registry                                                 |
| `timeline/`          | Optional          | Story timeline                                                         |
| `outlines/`          | Always            | Chapter outlines (plans)                                               |
| `drafts/`            | Always            | Chapter drafts (finished work)                                         |
| `items/`             | Optional          | Story key item archive — one `.md` per item                            |

#### 3.1 `project.yaml` — Project Metadata

```yaml
# project.yaml — Machine-readable project metadata
# Reasonix and MCP tools read this file to understand project basics

name: "以你的名"              # Project name (Chinese)
slug: "yideiming"            # Project slug (for API paths)
type: fanfic                  # original | fanfic
status: active                # active | archived | draft

# ── Fanfic-specific fields ──
fandom: "江湖风云录"
canon_version: "三部曲 + 外传"
canon_sources:
  - name: "江湖风云录·第一部"
    type: game
    year: 2021
  - name: "江湖风云录·外传"
    type: game
    year: 2017
canon_constraints:            # Canon red lines — deduction must not violate
  - "All canon event outcomes are immutable"
  - "Canon character death timing cannot be changed"
  - "Canon character core personalities cannot be reversed"

# ── Original fiction-specific fields ──
# world_type: "玄幻"
# magic_system: "元素体系"
# era: "架空古代"

# ── Common fields ──
protagonist: 古月              # Protagonist name
pov: "古月第三人称有限"        # Narrative perspective
language: zh-CN                # Writing language
phases:                        # Story phases
  - name: 降临期
    chapters: [1, 2]
  - name: 旁观期
    chapters: [3, 4, 5]
  - name: 石化期
    chapters: [6, 7, 8, 9, 10]
  - name: 回归期
    chapters: [11, 12, 13, 14]
  - name: 之后
    chapters: [15]

writing_constraints:           # Hard writing constraints
  chapter_word_count:
    min: 5000
    max: 9000
  forbidden:
    - "Light novel translation tone ('呐''嘛''呢')"
    - "Adverb-modified emotions ('gently said')"
    - "'不是…而是…' sentence pattern"
  dialogue_format:
    speech: '""'               # Dialogue in Chinese double quotes
    thought: "『』"             # Inner thoughts in angle brackets
  style_fingerprint: "远瞳《异常生物见闻录》"
```

#### 3.2 `project.ai.md` — Reasonix Operation Manual

AI operation manual for each project — must-read before writing. Structure:

```markdown
# project.ai.md — {Project Chinese Name}

> Type: {original/fanfic}
> Protagonist: {name}
> POV: {description}

## I. Writing Constraints
(chapter word count, language style, dialogue format, POV rules)

## II. Must-Read Files Before Writing
(listed in priority order)

## III. Post-Chapter Workflow
(status updates, foreshadowing registration, review, reverse-check profiles)

## IV. Character Quick Reference
(main characters + survival status + voice highlights)

## V. Phase Quick Reference
(core tasks and canon constraints per phase)
```

**Note**: `project.ai.md` should NOT duplicate info already in `project.yaml` (phases, word limits, etc.). They complement each other — `project.yaml` is machine-readable structured metadata, `project.ai.md` is AI-readable writing operations manual.

#### 3.3 `canon/` — Fanfic Canon Materials

```
canon/
├── timeline.md               # Canon event timeline (🔴🟡🟢 constraint labels)
├── world-overview.md         # Canon world overview
├── characters/               # Canon character facts (one file per person)
│   ├── 林青.md               #   - Canon age, history, key events
│   ├── 原作资料.md               #   - Does NOT include "in this story" interpretations
│   └── ...
├── relationships.yaml        # Canon character relationships (machine-readable)
│   # Example:
│   # - from: {Character A}
│   #   to: {Character B}
│   #   type: {relationship type}
│   #   canon_level: 🔴
│   #   note: {description}
└── sources.yaml              # Source references
    # - name: "{Work title}"
    #   type: {game/book}
    #   url: ...
```

#### 3.4 `worldbuilding/` — Worldbuilding

| Fanfic | Original Fiction |
|--------|-----------------|
| Non-canon settings that exist in this story (underground shelter, gene repair plan) | All worldbuilding (magic system, factions, history, geography) |

```
worldbuilding/
├── 世界观总览.md              # Core rules / magic system
├── 历史年表.md               # In-story history
├── 地理志.md                 # World map / locations
├── 种族与势力.md             # Factions
└── ...                       # Story-specific settings
```

#### 3.5 `characters/` — Character Profiles

```
characters/
├── {protagonist}.md              # Protagonist profile — includes evolution record
├── {important_character}.md      # If character direction differs from expectation → append "正文补充（第XXXX章后）"
├── {character_C}.md
└── ...
```

**Character Profile Template** (copy from `_shared/templates/character-profile.md`):

```markdown
# {Character Name}

> Project: {project-name}
> Type: {major / minor / extra}

## Basic Info
| Field | Value |
|-------|-------|
| Name | |
| Age | |
| Gender | |
| Affiliation | |

## Personality Traits
- 

## Voice
- Tone:
- Sentence style:
- Forbidden patterns:
- Seed dialogue:
  - ""
  - ""

## Relationships
| Character | Relationship Type | Trust (0-10) | Notes |
|-----------|-----------------|-------------|-------|
| | | | |

## Character Arc
(trajectory from beginning to end)

## Evolution Log
(appended per chapter if character direction diverges from profile)
```

#### 3.6 `states/` — Character State Tracking

```
states/
├── current.yaml               # Latest state (machine-readable, updated per chapter)
└── history/                   # Historical snapshots (optional — MCP tools auto-write)
    ├── ch0001.yaml
    └── ...
```

**`current.yaml` Format**:

```yaml
# states/current.yaml — Current character states
updated_at: "2026-06-09"
last_chapter: 6

characters:
  {protagonist}:
    location: "{current location}"
    physical: "{physical state}"
    psychology: "{psychological state}"
    items: ["{item 1}", "{item 2}"]
    summary: "{chapter character overview}"

  {important_character}:
    location: "{current location}"
    physical: "{physical state}"
    psychology: "{psychological state}"
    items: ["{item 1}"]
    summary: "{chapter character overview}"
```

#### 3.7 `foreshadowing.yaml` — Foreshadowing Registry

```yaml
# foreshadowing.yaml — Foreshadowing management system
foreshadows:
  - code: F001
    type: "🔮"
    description: "{foreshadowing description}"
    planted: {N}
    payoff: null
    status: "🌱"
    characters: ["{Character A}", "{Character B}"]
    
  - code: F002
    type: "🎭"
    description: "{foreshadowing description}"
    planted: {N}
    payoff: null
    status: "🌱"
    characters: ["{Character A}"]
```

#### 3.8 `outlines/` — Chapter Outlines

```
outlines/
├── 总纲.md                    # Story core, phase structure, ending vision
├── ch0001-降临.md             # Per-chapter outline
├── ch0002-这还是国内吗（上）.md
└── ...
```

**Outline Template** (`ch{NNNN}-{title}.md`):

```markdown
---
project: {project-slug}
type: outline
chapter: {number}
title: {title}
phase: {phase name}
goal: "Chapter goal — one sentence"
characters: [characters appearing]
---

# Chapter {NNNN}: {title}

## Scene 1: {scene name}
(brief description — environment, characters, what happens)

## Scene 2: {scene name}
...

## Scene 3: {scene name}
...

---

## Related Files
- outlines/总纲.md
- characters/xxx.md
- canon/xxx.md (fanfic)
```

#### 3.9 `drafts/` — Chapter Drafts

```
drafts/
├── ch0001-降临.md
├── ch0002-这还是国内吗（上）.md
└── ...
```

**Draft Template** (`ch{NNNN}-{title}.md`):

```markdown
---
project: {project-slug}
type: draft
chapter: {number}
title: {title}
phase: {phase name}
characters: [character list]
location: {main location}
word_count: {fill after writing}
status: draft                # draft → review → final
---

# Chapter {NNNN}: {title}

(body text, 5000-9000 words)
```

---

#### 3.10 `items/` — Item Archive

```
items/
├── 圣剑·誓约.md              # One file per item
├── 青铜怀表.md
└── ...
```

**Item Archive Template**:

```markdown
---
project: {project-slug}
name: {item-name}
item_type: {weapon/relic/token/tool/other}
current_holder: {current-holder}
current_status: {normal/damaged/lost/destroyed}
---

# {Item Name}

## Basic Info
| Field | Content |
|-------|---------|
| Name | |
| Type | |
| Appearance | |
| Usage/Ability | |

## Origin
- Creator/Discoverer:
- First appearance chapter:
- Origin event:

## Ownership History
| Chapter | Holder | Event |
|---------|--------|-------|
| ch{N} | {CharA} | First appearance |
| ch{N} | {CharB} | Gift/loss/seizure |
```

> **Note**: Item archive is optional. The `items` string array in `states/current.yaml` records the character's
> currently held item names — item names should match filenames in `items/` for cross-referencing.

---

### IV. Naming Conventions

| Object             | Format                               | Example                             |
|--------------------|--------------------------------------|-------------------------------------|
| Project directory  | `{Chinese name}` or `{English slug}` | `{project-name}` / `{project-slug}` |
| Outline file       | `ch{NNNN}-{title}.md`                | `ch0005-第一个夜晚.md`                   |
| Draft file         | `ch{NNNN}-{title}.md`                | `ch0005-第一个夜晚.md`                   |
| Character profile  | `{name}.md`                          | `林青.md`                             |
| Item archive       | `{item-name}.md`                     | `圣剑·誓约.md`                          |
| Canon character    | `{name}.md`                          | `林青.md`                             |
| State snapshot     | `ch{NNNN}.yaml`                      | `ch0006.yaml`                       |
| Foreshadowing code | `F{NNN}`                             | `F001`                              |
| Worldbuilding file | `{topic}.md`                         | `魔法体系.md`                           |

**Note**: Outlines and drafts use the same filename format but belong to **different directories** (`outlines/` vs `drafts/`) — no confusion.

---

### V. Fanfic vs Original File Comparison

```
Project root:

Fanfic ({project-name})                  Original ({project-name})
├── project.yaml                    ├── project.yaml
├── project.ai.md                   ├── project.ai.md
├── canon/          ← fanfic only   │
│   ├── timeline.md                │
│   ├── world-overview.md          │
│   ├── characters/                │
│   ├── relationships.yaml         │
│   └── sources.yaml               │
├── worldbuilding/ ← non-canon ext ├── worldbuilding/ ← all settings
│   ├── {setting1}.md              │   ├── world-overview.md
│   ├── {setting2}.md              │   ├── magic-system.md
│   ├── {setting3}.md              │   ├── factions.md
│   └── {setting4}.md              │   ├── history.md
│                                   │   └── geography.md
├── characters/                     ├── characters/
├── states/                         ├── states/
├── foreshadowing.yaml              ├── foreshadowing.yaml
├── timeline/                       ├── timeline/
├── outlines/                       ├── outlines/
├── items/     ← optional           ├── items/   ← optional
└── drafts/                         └── drafts/
```

---

### VI. New Project Checklist

1. [ ] Create project directory under `novel-vault/`
2. [ ] Copy `project-meta.yaml` from `_shared/templates/` → rename to `project.yaml` → fill in
3. [ ] Copy `project.ai.md` from `_shared/templates/` → fill in writing constraints
4. [ ] Create `outlines/`, `drafts/`, `characters/`, `states/` directories
5. [ ] If fanfic: create `canon/` directory → import canon materials
6. [ ] If original: create `worldbuilding/` directory → start building world
7. [ ] Create `foreshadowing.yaml` (empty array)
8. [ ] Create `states/current.yaml` (empty)
9. [ ] Define main characters → write one `.md` each in `characters/`
10. [ ] If key items exist: create `items/` directory → write one `.md` per item in `items/`
11. [ ] Call MCP `project_init` tool → create project record on server
12. [ ] Start writing `总纲.md` → begin writing
