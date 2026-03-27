# Android Tools Reference

AndroidForClaw 提供的 Android 专属工具集。

---

## 📸 观察工具

### screenshot

截取当前屏幕。

**参数**: 无

**返回**:
```json
{
  "success": true,
  "content": "截图已保存: /sdcard/AndroidForClaw/screenshots/2026-03-06_123456.jpg"
}
```

**实现**:
- 使用 MediaProjection API
- 需要用户授权 (一次性)
- 保存到 `/sdcard/AndroidForClaw/screenshots/`

**用法**:
```json
{
  "name": "screenshot",
  "arguments": {}
}
```

**注意事项**:
- 截图前会自动隐藏悬浮窗
- 截图后恢复悬浮窗
- 每次截图约 500ms-1s

---

### get_ui_tree

获取当前界面的 UI 树结构。

**参数**: 无

**返回**:
```json
{
  "success": true,
  "content": "UI Tree:\n├── FrameLayout\n│   ├── TextView: \"标题\"\n│   └── Button: \"确定\""
}
```

**实现**:
- 使用 AccessibilityService
- 遍历 AccessibilityNodeInfo 树
- 生成层级结构文本

**用法**:
```json
{
  "name": "get_ui_tree",
  "arguments": {}
}
```

**适用场景**:
- 查找特定控件
- 理解界面结构
- 调试 UI 问题

**限制**:
- 某些 App 可能禁用 Accessibility
- WebView 内容可能无法获取完整树

---

## 👆 操作工具

### tap

点击屏幕坐标。

**参数**:
```typescript
{
  x: number,  // X 坐标
  y: number   // Y 坐标
}
```

**返回**:
```json
{
  "success": true,
  "content": "已点击坐标 (540, 1000)"
}
```

**实现**:
- 使用 AccessibilityService.dispatchGesture()
- GestureDescription: 单点触摸，duration=50ms

**用法**:
```json
{
  "name": "tap",
  "arguments": {
    "x": 540,
    "y": 1000
  }
}
```

**坐标系**:
- 原点 (0, 0) 在左上角
- 单位: 像素 (px)
- 需要先 screenshot 确定坐标

**常见问题**:
- 坐标超出屏幕范围 → 返回错误
- Accessibility 权限未授予 → 返回错误

---

### swipe

滑动手势。

**参数**:
```typescript
{
  start_x: number,      // 起始 X
  start_y: number,      // 起始 Y
  end_x: number,        // 结束 X
  end_y: number,        // 结束 Y
  duration_ms: number   // 持续时间 (可选，默认 300ms)
}
```

**返回**:
```json
{
  "success": true,
  "content": "已执行滑动: (540, 1000) → (540, 500), 用时 300ms"
}
```

**用法**:
```json
{
  "name": "swipe",
  "arguments": {
    "start_x": 540,
    "start_y": 1500,
    "end_x": 540,
    "end_y": 500,
    "duration_ms": 300
  }
}
```

**常见滑动**:
```kotlin
// 向上滑动 (查看更多内容)
swipe(540, 1500, 540, 500, 300)

// 向下滑动 (下拉刷新)
swipe(540, 500, 540, 1500, 300)

// 向左滑动 (翻页)
swipe(800, 1000, 200, 1000, 300)

// 向右滑动
swipe(200, 1000, 800, 1000, 300)
```

---

### type

输入文字。

**参数**:
```typescript
{
  text: string  // 要输入的文字
}
```

**返回**:
```json
{
  "success": true,
  "content": "已输入文字: Hello"
}
```

**实现**:
- 使用 AccessibilityService
- 通过 `performAction(ACTION_SET_TEXT)` 或剪贴板

**用法**:
```json
{
  "name": "type",
  "arguments": {
    "text": "Hello World"
  }
}
```

**注意**:
- 需要先点击输入框获得焦点
- 某些 App 可能禁用 Accessibility 输入
- 支持中文、emoji 等 Unicode 字符

---

### long_press

长按屏幕坐标。

**参数**:
```typescript
{
  x: number,          // X 坐标
  y: number,          // Y 坐标
  duration_ms: number // 持续时间 (可选，默认 1000ms)
}
```

**返回**:
```json
{
  "success": true,
  "content": "已长按坐标 (540, 1000), 持续 1000ms"
}
```

**用法**:
```json
{
  "name": "long_press",
  "arguments": {
    "x": 540,
    "y": 1000,
    "duration_ms": 1000
  }
}
```

**适用场景**:
- 长按弹出菜单
- 选择文本
- 删除操作

---

## 🧭 导航工具

### home

返回主屏幕。

**参数**: 无

**返回**:
```json
{
  "success": true,
  "content": "已返回主屏幕"
}
```

**实现**:
```kotlin
performGlobalAction(GLOBAL_ACTION_HOME)
```

**用法**:
```json
{
  "name": "home",
  "arguments": {}
}
```

---

### back

返回上一页。

**参数**: 无

**返回**:
```json
{
  "success": true,
  "content": "已执行返回操作"
}
```

**实现**:
```kotlin
performGlobalAction(GLOBAL_ACTION_BACK)
```

**用法**:
```json
{
  "name": "back",
  "arguments": {}
}
```

---

### open_app

打开指定应用。

**参数**:
```typescript
{
  package_name: string  // 包名，如 "com.tencent.mm"
}
```

**返回**:
```json
{
  "success": true,
  "content": "已打开应用: 微信"
}
```

**实现**:
```kotlin
val intent = packageManager.getLaunchIntentForPackage(packageName)
context.startActivity(intent)
```

**用法**:
```json
{
  "name": "open_app",
  "arguments": {
    "package_name": "com.tencent.mm"
  }
}
```

**常见包名**:
```
微信: com.tencent.mm
QQ: com.tencent.mobileqq
支付宝: com.eg.android.AlipayGphone
淘宝: com.taobao.taobao
抖音: com.ss.android.ugc.aweme
Chrome: com.android.chrome
设置: com.android.settings
```

---

### recent_apps

打开最近任务列表。

**参数**: 无

**返回**:
```json
{
  "success": true,
  "content": "已打开最近任务"
}
```

**实现**:
```kotlin
performGlobalAction(GLOBAL_ACTION_RECENTS)
```

**用法**:
```json
{
  "name": "recent_apps",
  "arguments": {}
}
```

---

## ⚙️ 系统工具

### wait

等待/延迟。

**参数**:
```typescript
{
  duration_ms: number  // 等待时长 (毫秒)
}
```

**返回**:
```json
{
  "success": true,
  "content": "等待 2000ms 完成"
}
```

**用法**:
```json
{
  "name": "wait",
  "arguments": {
    "duration_ms": 2000
  }
}
```

**适用场景**:
- 等待应用加载
- 等待动画完成
- 等待网络请求

**建议**:
- 短暂等待: 500-1000ms
- 应用启动: 2000-3000ms
- 网络加载: 3000-5000ms

---

### stop

停止 Agent 执行。

**参数**: 无

**返回**:
```json
{
  "success": true,
  "content": "停止执行"
}
```

**用法**:
```json
{
  "name": "stop",
  "arguments": {}
}
```

**效果**:
- Agent Loop 立即退出
- 返回当前状态
- Session 保存

**适用场景**:
- 任务完成
- 遇到无法解决的问题
- 用户取消操作

---

### notification

发送 Android 通知。

**参数**:
```typescript
{
  title: string,    // 通知标题
  message: string   // 通知内容
}
```

**返回**:
```json
{
  "success": true,
  "content": "通知已发送: 任务完成"
}
```

**用法**:
```json
{
  "name": "notification",
  "arguments": {
    "title": "任务完成",
    "message": "已成功打开微信"
  }
}
```

**效果**:
- 显示在 Android 通知栏
- 可点击打开 App

---

## 🔧 工具开发

### 添加新工具

**1. 创建 Skill 类**:

```kotlin
// app/src/main/java/com/agent/mobile/agent/tools/YourSkill.kt
class YourSkill(private val context: Context) : Skill {
    override val name = "your_skill"
    override val description = "Your skill description"

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "param1" to PropertySchema("string", "Parameter description")
                    ),
                    required = listOf("param1")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        val param1 = args["param1"] as? String
            ?: return SkillResult.error("Missing param1")

        return try {
            // Your implementation here
            SkillResult.success("Operation successful")
        } catch (e: Exception) {
            SkillResult.error("Failed: ${e.message}")
        }
    }
}
```

**2. 注册到 SkillRegistry**:

```kotlin
// app/src/main/java/com/agent/mobile/agent/tools/SkillRegistry.kt
class SkillRegistry(private val context: Context) {
    init {
        // ... existing skills
        register(YourSkill(context))
    }
}
```

**3. 更新 System Prompt** (可选):

```kotlin
// app/src/main/java/com/agent/mobile/agent/context/ContextBuilder.kt
// 在工具列表中添加使用指南
```

---

## 📚 相关文档

- [Agent Loop](../concepts/agent-loop.md) - 工具如何被调用
- [Custom Skills](./custom-skills.md) - 创建自定义技能
- [Testing Guide](../debug/testing.md) - 测试工具

---

**Last Updated**: 2026-03-27
**Total Tools**: 40 (16 Universal + 24 Android-specific)
