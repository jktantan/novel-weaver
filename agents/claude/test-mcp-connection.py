"""NOVEL-MCP-SERVER 连接测试脚本"""
import urllib.request as r
import json, re, sys

MCP_HOST = "192.168.88.10"
MCP_PORT = 8883

def mcp_call(method, args=None):
    h = r.Request(f"http://{MCP_HOST}:{MCP_PORT}/mcp")
    h.add_header("Content-Type", "application/json")
    h.add_header("Accept", "text/event-stream, application/json")
    body = json.dumps({
        "jsonrpc": "2.0", "id": 1,
        "method": "tools/call",
        "params": {"name": method, "arguments": args or {}}
    }).encode()
    resp = r.urlopen(h, data=body, timeout=10)
    return json.loads(resp.read().decode())

# 步骤 1: 健康检查
print("[1/3] 检查服务健康状态...")
try:
    resp = r.urlopen(f"http://{MCP_HOST}:{MCP_PORT}/health", timeout=5)
    print(f"  {resp.read().decode()} - OK")
except Exception as e:
    print(f"  [失败] {e}")
    sys.exit(1)

# 步骤 2: 工具列表
print("\n[2/3] 获取 MCP 工具列表...")
h = r.Request(f"http://{MCP_HOST}:{MCP_PORT}/mcp")
h.add_header("Content-Type", "application/json")
h.add_header("Accept", "text/event-stream, application/json")
body = json.dumps({"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}).encode()
resp = r.urlopen(h, data=body, timeout=10)
data = json.loads(resp.read().decode())
tools = data["result"]["tools"]
print(f"  工具数: {len(tools)}")
for t in tools:
    print(f"  - {t['name']}")
print("  - OK")

# 步骤 3: 创建+删除项目
print("\n[3/3] 测试创建并删除项目...")
result = mcp_call("project_init", {"name": "连接测试-可删除", "type": "fanfic"})
text = result["result"]["content"][0]["text"]
pid = re.search(r"[0-9a-f-]{36}", text)
if pid:
    pid = pid.group(0)
    result2 = mcp_call("project_delete", {"projectId": pid})
    print(f"  projectId: {pid}")
    print(f"  删除: {result2['result']['content'][0]['text'][:60]}")
    print("  ✅ 通过")
else:
    print(f"  [失败] {text}")
    sys.exit(1)

print("\n================================")
print(" 全部测试通过！")
print("================================")
