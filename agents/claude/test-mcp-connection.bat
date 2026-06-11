@echo off
REM NOVEL-MCP-SERVER 连接测试脚本
REM 用法: test-mcp-connection.bat

set MCP_HOST=192.168.88.10
set MCP_PORT=8883

echo ============================================
echo  NOVEL-MCP-SERVER 连接测试
echo  %MCP_HOST%:%MCP_PORT%
echo ============================================
echo.

REM 健康检查
echo [1/3] 检查服务健康状态...
curl -s http://%MCP_HOST%:%MCP_PORT%/health
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [失败] 无法连接到 %MCP_HOST%:%MCP_PORT%
    pause
    exit /b 1
)
echo.
echo - OK
echo.

REM 运行 Python 测试（工具列表 + 创建/删除）
python "%~dp0test-mcp-connection.py"

pause
