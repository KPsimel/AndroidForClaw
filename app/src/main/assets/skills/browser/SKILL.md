---
name: browser
description: Web browser automation through browserforclaw
metadata:
  {
    "openclaw": {
      "always": false,
      "emoji": "🌐",
      "version": "2.0.0",
      "category": "automation"
    }
  }
---

# BrowserForClaw Integration

Control the browserforclaw browser to perform web automation tasks.

## 🎯 When to Use

Use this skill when you need to:

✅ **Web search** - Search on Baidu, Google, or other search engines
✅ **Web automation** - Interact with websites and web apps
✅ **Data extraction** - Scrape content from web pages
✅ **Form filling** - Submit web forms programmatically
✅ **Testing web apps** - Validate web application behavior
✅ **API interaction** - Test web services through browser

⚠️ **IMPORTANT**: When user asks to "用浏览器搜索" or "open browser and search":
- DO NOT use `open_app` to launch Chrome/other browser apps
- Instead, use the `browser` tool directly (it controls browserforclaw)
- browserforclaw is already running in the background, no need to launch it

## 🌐 Browser Tool

The `browser` tool provides unified access to all browserforclaw capabilities through a single interface.

### Usage Pattern

```json
{
  "operation": "navigate|click|type|get_content|wait|scroll|execute|press|screenshot|get_cookies|set_cookies|hover|select",
  ...operation-specific parameters
}
```

## 📋 Supported Operations

### 1. navigate - Navigate to URL

```json
{
  "operation": "navigate",
  "url": "https://example.com",
  "waitMs": 2000
}
```

### 2. click - Click element

```json
{
  "operation": "click",
  "selector": "#login-button",
  "index": 0
}
```

### 3. type - Input text

```json
{
  "operation": "type",
  "selector": "input[name='q']",
  "text": "search query",
  "clear": true,
  "submit": false
}
```

### 4. get_content - Get page content

```json
{
  "operation": "get_content",
  "format": "text",
  "selector": "#main-content"
}
```

Formats: `text` (default), `html`, `markdown`

### 5. wait - Wait for condition

Wait for time:
```json
{"operation": "wait", "timeMs": 2000}
```

Wait for element:
```json
{"operation": "wait", "selector": "#content", "timeout": 5000}
```

Wait for text:
```json
{"operation": "wait", "text": "Welcome", "timeout": 5000}
```

Wait for URL:
```json
{"operation": "wait", "url": "/dashboard", "timeout": 5000}
```

### 6. scroll - Scroll page

```json
{"operation": "scroll", "direction": "down"}
```

Directions: `up`, `down`, `top`, `bottom`

Or scroll by pixels:
```json
{"operation": "scroll", "x": 0, "y": 500}
```

### 7. execute - Execute JavaScript

```json
{
  "operation": "execute",
  "script": "document.title"
}
```

### 8. press - Press key

```json
{"operation": "press", "key": "Enter"}
```

Supported keys: `Enter`, `Backspace`, `Tab`, `Escape`, `ArrowUp`, `ArrowDown`, `ArrowLeft`, `ArrowRight`

### 9. screenshot - Capture screenshot

```json
{
  "operation": "screenshot",
  "fullPage": true,
  "format": "png"
}
```

### 10. get_cookies/set_cookies - Cookie operations

Get cookies:
```json
{"operation": "get_cookies"}
```

Set cookies:
```json
{
  "operation": "set_cookies",
  "cookies": ["session_id=abc123; path=/"]
}
```

### 11. hover - Hover over element

```json
{
  "operation": "hover",
  "selector": ".dropdown-menu"
}
```

### 12. select - Select dropdown option

```json
{
  "operation": "select",
  "selector": "select[name='country']",
  "values": ["CN"]
}
```

## 🎯 Best Practices

### 1. Always Navigate First

Before interacting with a page, navigate to the URL:

```
1. browser(operation="navigate", url="https://example.com", waitMs=2000)
2. browser(operation="wait", selector="#content", timeout=5000)
3. browser(operation="click", selector="#button")
```

### 2. Wait for Elements

After navigation or actions that trigger page changes, wait for elements to appear:

```
browser(operation="wait", selector="#search-results", timeout=5000)
```

### 3. Verify Actions

Get content after important actions to verify success:

```
1. browser(operation="type", selector="input", text="query", submit=true)
2. browser(operation="wait", timeMs=1000)
3. browser(operation="get_content", format="text")
```

### 4. Handle Timeouts

Always specify reasonable timeouts for wait operations:

```
browser(operation="wait", selector="#slow-element", timeout=10000)
```

## 🔄 Common Workflows

### Baidu Search (百度搜索)

```
1. browser(operation="navigate", url="https://www.baidu.com", waitMs=2000)
2. browser(operation="wait", selector="#kw", timeout=5000)
3. browser(operation="type", selector="#kw", text="openclaw", submit=true)
4. browser(operation="wait", timeMs=2000)
5. browser(operation="get_content", format="text")
```

**Example user request**: "用浏览器去百度搜一下 openclaw"
- Navigate to baidu.com
- Wait for search box (#kw)
- Type "openclaw" and submit
- Wait for results to load
- Get and report search results

### Google Search

```
1. Navigate to google.com
2. Wait for search box
3. Type query and submit
4. Wait for results
5. Get content or click result
```

### Form Submission

```
1. Navigate to form page
2. Wait for form to load
3. Type into each field
4. Click submit button
5. Wait for confirmation
6. Verify success
```

### Data Extraction

```
1. Navigate to target page
2. Wait for content to load
3. Get page content
4. Execute JavaScript if needed for complex data
```

## ⚠️ Important Notes

- **Network**: Ensure browserforclaw has internet connectivity
- **Timing**: Add appropriate waits after navigation and actions
- **Selectors**: Use specific CSS selectors to avoid ambiguity
- **Errors**: Browser operations may timeout - handle gracefully

## 🔗 Integration

browserforclaw runs as a separate Android app and communicates via HTTP API (localhost:8080).

## 🔍 Troubleshooting

### Issue: Browser Not Responding

**Check**: Is browserforclaw app running?
```
# Verify with screenshot or app list
# Launch browserforclaw if needed
```

### Issue: Element Not Found

**Solution**: Wait for element, check selector
```
browser(operation="wait", selector="#target", timeout=5000)
browser(operation="click", selector="#target")
```

### Issue: Navigation Timeout

**Solution**: Increase timeout for slow pages
```
browser(operation="navigate", url="...", waitMs=5000)
```

### Issue: JavaScript Execution Fails

**Debug**: Check script syntax, return value
```
browser(operation="execute", script="return document.title")
```

---

**BrowserForClaw** - Web automation for AndroidForClaw 🌐📱
