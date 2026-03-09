# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BrowserForClaw is a **two-part system** for browser automation on Android:

1. **Android App** (separate project) - Provides HTTP API server (port 8765) with 13 browser automation tools
2. **SKILL.md File** (this repo) - Teaches AI agents how to use those tools

**Note**: This `browserforclaw` directory contains **reference documentation only** for the browser automation skills.

## Architecture

### HTTP REST API Flow
```
AI Agent/Client
    ↓ HTTP POST to localhost:8765/api/browser/execute
SimpleBrowserHttpServer.kt (receives JSON)
    ↓ Routes to tool
BrowserToolsExecutor.kt
    ↓ Executes specific tool
BrowserNavigateTool.kt / BrowserClickTool.kt / etc. (13 tools)
    ↓ Controls
BrowserManager.kt (manages WebView)
    ↓
Android WebView (real browser)
```

### Request/Response Format
```json
// Request
{
  "tool": "browser_navigate",
  "args": {"url": "https://example.com"}
}

// Response
{
  "success": true,
  "data": {"url": "https://example.com", "title": "Example Domain"},
  "error": null
}
```

### Key Components in einkbro project

- **SimpleBrowserHttpServer.kt** - HTTP server on port 8765, handles /health and /api/browser/execute
- **BrowserToolsExecutor.kt** - Routes tool name to correct implementation
- **tools/** - 13 tool implementations (BrowserNavigateTool, BrowserClickTool, etc.)
- **BrowserManager.kt** - WebView control, JavaScript execution, UI thread handling
- **ToolResult.kt** - Standard response model

### The 13 Browser Tools

| Category | Tools |
|----------|-------|
| Navigation | navigate |
| Interaction | click, type, hover, select |
| Content | get_content (text/html/markdown), screenshot |
| Keyboard | press |
| Scrolling | scroll |
| Timing | wait |
| JavaScript | execute |
| Cookies | get_cookies, set_cookies |

## Common Development Tasks

### Building APK
```bash
# Build in the separate BrowserForClaw project
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-universal-debug.apk
```

### Installing and Testing
```bash
# Install APK
adb install app-universal-debug.apk

# Start app (HTTP server auto-starts on port 8765)
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity

# Setup port forwarding
adb forward tcp:8765 tcp:8765

# Test health check
curl http://localhost:8765/health

# Test browser tool
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_navigate","args":{"url":"https://example.com"}}'
```

### Viewing Logs
```bash
# HTTP server logs
adb logcat | grep BrowserHttpServer

# Tool execution logs
adb logcat | grep BrowserTools

# All browser control logs
adb logcat | grep -E "(BrowserHttpServer|BrowserTools|BrowserManager)"
```

### Testing Individual Tools
```bash
# See HTTP_API_TEST_REPORT.md for curl commands for all 13 tools
# Example:
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_get_content","args":{"format":"text"}}'
```

## Important Rules

### Code Modification Guidelines

1. **BrowserForClaw code is in a separate project**
   - This directory contains documentation only
   - Modifications require rebuilding the separate APK
   - Always test after modifications

2. **This `browserforclaw` directory is documentation-only**
   - Contains SKILL.md, README.md, guides, etc.
   - Cannot be compiled standalone
   - Safe to modify documentation files here

3. **When moving files, ONLY move - don't modify**
   - User may say "move these files" - do NOT change code during moves
   - Verify compilation after moves
   - If code stops compiling, revert immediately

### JSON Parsing Gotcha

When handling JSON in SimpleBrowserHttpServer.kt, remember to convert JSONArray to List:

```kotlin
// CORRECT - converts JSONArray to List
is org.json.JSONArray -> {
    val list = mutableListOf<Any?>()
    for (i in 0 until it.length()) {
        list.add(it.opt(i))
    }
    list
}

// WRONG - passes JSONArray object directly
is org.json.JSONArray -> it // Tools expect List, not JSONArray!
```

This is especially important for `browser_set_cookies` which expects `List<String>`.

### Package Names

- Use `com.example.browser` for example code
- Actual app uses `info.plateaukao.einkbro` (EinkBro base)
- BrowserForClaw tools are in `info.plateaukao.einkbro.browser.control`

## Skills System Integration

### How SKILL.md Works

1. **AI Agent loads SKILL.md** from skills directory (AgentSkills.io format)
2. **Agent learns** what tools exist and how to use them
3. **Agent decides** which tools to call for a given task
4. **Agent calls** tools via HTTP API

### SKILL.md Structure
```markdown
---
name: browser-automation
description: Web browser automation via HTTP API
metadata: { "openclaw": { "always": false, "emoji": "🌐" } }
---

# Documentation
- Tool descriptions with parameters
- Common patterns (Google Search, Form Filling, etc.)
- Important rules (always wait after navigate, etc.)
- HTTP client setup code
- Troubleshooting
```

### Integration Pattern

**AI Agent Framework** (like OpenClaw/phoneforclaw):
1. Copy SKILL.md to `app/src/main/assets/skills/browser-automation/SKILL.md`
2. Implement BrowserHttpClient.kt (see PHONEFORCLAW_HTTP_CLIENT.md)
3. Register tools in tool registry
4. Agent automatically knows how to use browser

## CSS Selectors

Tools use standard CSS selector syntax:
- `input[name='q']` - attribute selector
- `#search-button` - ID selector
- `.article-title` - class selector
- `button.submit` - element + class

**Baidu search example**: Use `#index-kw` (not `#kw`) for search input.

## Troubleshooting

### HTTP Server Not Starting
```bash
# Check if app is running
adb shell ps | grep einkbro

# Force restart
adb shell am force-stop info.plateaukao.einkbro
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity
```

### Connection Refused
```bash
# Reset port forwarding
adb forward --remove tcp:8765
adb forward tcp:8765 tcp:8765

# Verify forwarding
adb forward --list
```

### Tool Execution Fails
- Ensure page loaded (use `browser_wait`)
- Verify CSS selector with browser DevTools
- Check logs for error messages
- JavaScript-heavy sites need longer wait times

## Documentation Files

- **README.md** - Project homepage
- **UNDERSTANDING.md** - Explains two-part architecture
- **SKILL.md** - AI agent skill definition (14KB)
- **QUICK_START.md** - 3-minute setup guide
- **INTEGRATION.md** - Integration guide with complete client code
- **TOOLS_REFERENCE.md** - Complete API reference for 13 tools
- **FAQ.md** - 30+ common questions
- **HTTP_API_TEST_REPORT.md** - Test methodology and results
- **LIVE_TEST_RESULTS.md** - Real device test results
- **PHONEFORCLAW_HTTP_CLIENT.md** - Example HTTP client implementation
- **PROJECT_SUMMARY.md** - Chinese project summary
- **CONTRIBUTING.md** - Contribution guidelines
- **LICENSE** - MIT License

## Git Workflow

Current repository has 6 clean commits:
1. Initial commit
2. 添加完整的开发历史记录
3. 整理文档并准备开源
4. 添加开源必备文件
5. 添加 UNDERSTANDING.md
6. 添加 FAQ

When making changes:
- Keep commits focused and atomic
- Use descriptive Chinese or English commit messages
- Test changes before committing

## OpenClaw API Alignment

BrowserForClaw is 95% aligned with OpenClaw Browser API:
- Same tool names (browser_navigate, browser_click, etc.)
- Same parameter names and types
- Same response format (ToolResult with success/data/error)
- OpenClaw skills can be ported with minimal changes

## Key Technical Constraints

- **Android API 24+** required
- **WebView-based** - requires UI context (not headless)
- **Single instance** per device
- **No authentication** by default (localhost/trusted networks only)
- **Kotlin Coroutines** for async operations
- **Main thread** required for WebView operations (handled by BrowserManager)

## Testing Philosophy

All 13 tools have been tested with:
- Real Android device (SDK 36)
- Real-world scenarios (Baidu search, Google search, form filling)
- Success rate: 100%
- Test date: 2026-03-06

See LIVE_TEST_RESULTS.md for complete test results.
