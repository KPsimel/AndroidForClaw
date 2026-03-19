# BrowserForClaw MCP Server

将 BrowserForClaw 的 Android 浏览器自动化能力通过 MCP (Model Context Protocol) 暴露给 Claude Desktop 等 AI 客户端。

## 快速开始

### 1. 安装 MCP Bridge

```bash
cd extensions/BrowserForClaw/mcp-bridge
npm install
```

### 2. 配置 BrowserForClaw

确保 BrowserForClaw 已安装并运行：

```bash
# 安装 APK（如果尚未安装）
adb install extensions/BrowserForClaw/android-project/app/build/outputs/apk/release/app-universal-release.apk

# 启动应用
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity

# 设置端口转发
adb forward tcp:58765 tcp:58765

# 验证服务运行
curl http://localhost:58765/health
# 应返回: {"status":"ok"}
```

### 3. 配置 Claude Desktop

**macOS**: 编辑 `~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows**: 编辑 `%APPDATA%\Claude\claude_desktop_config.json`

**Linux**: 编辑 `~/.config/Claude/claude_desktop_config.json`

添加以下配置：

```json
{
  "mcpServers": {
    "browserforclaw": {
      "command": "node",
      "args": [
        "/Users/qiao/file/forclaw/phoneforclaw/extensions/BrowserForClaw/mcp-bridge/index.js"
      ],
      "env": {
        "BROWSER_API_URL": "http://localhost:58765",
        "LOG_LEVEL": "info"
      }
    }
  }
}
```

**注意**: 将路径 `/Users/qiao/file/forclaw/phoneforclaw/` 替换为你的实际路径。

### 4. 重启 Claude Desktop

重启 Claude Desktop 后，MCP 服务器会自动启动，你可以在 Claude 中使用以下工具：

## 可用工具 (13 个)

| 工具 | 描述 |
|------|------|
| `browser_navigate` | 导航到指定 URL |
| `browser_click` | 点击页面元素 |
| `browser_type` | 在输入框中输入文本 |
| `browser_get_content` | 获取页面内容 (text/html/markdown) |
| `browser_screenshot` | 截取页面截图 |
| `browser_scroll` | 滚动页面 |
| `browser_wait` | 等待条件或时间 |
| `browser_execute` | 执行 JavaScript 代码 |
| `browser_hover` | 悬停在元素上 |
| `browser_select` | 选择下拉选项 |
| `browser_press` | 按下键盘按键 |
| `browser_get_cookies` | 获取 Cookies |
| `browser_set_cookies` | 设置 Cookies |

## 使用示例

在 Claude Desktop 中，你可以这样使用：

```
你: 帮我打开百度搜索 "天气预报"

Claude 会自动调用:
1. browser_navigate(url="https://www.baidu.com")
2. browser_type(selector="#kw", text="天气预报")
3. browser_click(selector="#su")
4. browser_get_content(format="text")
```

## 架构

```
Claude Desktop
    ↓ (MCP Protocol - JSON-RPC 2.0 over stdio)
MCP Bridge (Node.js)
    ↓ (HTTP REST API)
BrowserForClaw (Android)
    ↓
WebView Browser
```

## 故障排除

### 连接被拒绝

```bash
# 检查 BrowserForClaw 是否运行
adb shell ps | grep einkbro

# 重启应用
adb shell am force-stop info.plateaukao.einkbro
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity

# 重新设置端口转发
adb forward --remove-all
adb forward tcp:58765 tcp:58765
```

### MCP Bridge 未启动

```bash
# 查看 Claude Desktop 日志
# macOS: ~/Library/Logs/Claude/
# Windows: %APPDATA%\Claude\logs\
# Linux: ~/.config/Claude/logs/

# 手动测试 MCP Bridge
cd extensions/BrowserForClaw/mcp-bridge
node index.js
```

### 依赖安装失败

```bash
# 清理并重新安装
cd extensions/BrowserForClaw/mcp-bridge
rm -rf node_modules package-lock.json
npm install
```

## 环境变量

- `BROWSER_API_URL`: BrowserForClaw API 地址 (默认: `http://localhost:58765`)
- `LOG_LEVEL`: 日志级别 `info`/`debug` (默认: `info`)

## 技术栈

- **MCP SDK**: `@modelcontextprotocol/sdk` v0.5.0
- **HTTP Client**: `axios` v1.6.0
- **Transport**: stdio (标准输入/输出)
- **Protocol**: MCP (Model Context Protocol)

## 相关文档

- [MCP 实现指南](../MCP_SERVER_IMPLEMENTATION.md)
- [BrowserForClaw 文档](../README.md)
- [工具参考](../TOOLS_REFERENCE.md)
- [MCP 官方文档](https://modelcontextprotocol.io/)

## 许可证

MIT License - forClaw Project
