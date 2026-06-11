# Claude 客户端配置 — NOVEL-MCP-SERVER

Claude Code（CLI）和 Claude Desktop（GUI）两个版本，配置方式不同。

---

## Claude Code (CLI)

在项目 `.claude/settings.local.json` 中添加：

```json
{
  "mcpServers": {
    "novel-mcp-server": {
      "url": "http://192.168.88.10:8883/mcp"
    }
  }
}
```

同时建议加权限放行：

```json
"mcp__novel-mcp-server__*"
```

模板见 `settings-template.json`。

## Claude Desktop (GUI)

将 `claude_desktop_config.template.json` 的内容合并到：

| 平台 | 路径 |
|------|------|
| Windows | `%APPDATA%\Claude\claude_desktop_config.json` |
| macOS | `~/Library/Application Support/Claude/claude_desktop_config.json` |
| Linux | `~/.config/Claude/claude_desktop_config.json` |

修改后重启 Claude Desktop。

## 使用

配置完成后，直接说"初始化项目"即可。

## 数据备份与恢复

调 `mcp__novel-mcp-server__project_export` 导出项目全部数据为 JSON：
- 客户端拿到 JSON 后写入本地文件（`chapters/`、`characters/`、`foreshadowing.yaml` 等）

调 `mcp__novel-mcp-server__project_import` 从 JSON 恢复项目全部数据：
- 客户端读取本地文件，组装成 export 同结构的 JSON，传给 import

文件和数据库互为镜像，任一损坏可从另一方恢复。
