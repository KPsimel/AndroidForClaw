# Sessions 系统详细对比

**对比日期**: 2026-03-08
**结论**: ✅ **完全对齐** (100%)

---

## 📊 对比总览

| 特性 | OpenClaw | AndroidForClaw | 对齐状态 |
|------|----------|---------------|---------|
| **JSONL 格式** | ✅ | ✅ | ✅ 完全对齐 |
| **sessions.json 索引** | ✅ | ✅ | ✅ 完全对齐 |
| **Session 持久化** | ✅ | ✅ | ✅ 完全对齐 |
| **Session 创建** | ✅ | ✅ | ✅ 完全对齐 |
| **Session 列表** | ✅ | ✅ | ✅ 完全对齐 |
| **Session 预览** | ✅ | ✅ | ✅ 完全对齐 |
| **Session 重置** | ✅ | ✅ | ✅ 完全对齐 |
| **Session 删除** | ✅ | ✅ | ✅ 完全对齐 |
| **Session Patch** | ✅ | ✅ | ✅ 完全对齐 |
| **自动压缩** | ✅ | ✅ | ✅ 完全对齐 |
| **自动清理** | ✅ | ✅ | ✅ 完全对齐 |
| **导入/导出** | ⚠️ | ✅ | ✅ 超越 OpenClaw |

**对齐率**: 12/12 = **100%** ✅

---

## 1. 核心实现对比

### 1.1 JsonlSessionStorage.kt

**功能**: JSONL Session 存储引擎

**对齐 OpenClaw 架构**:
```kotlin
// 对齐 OpenClaw: agents/main/sessions/
private const val SESSIONS_DIR = "/sdcard/.androidforclaw/agents/main/sessions"
private const val SESSIONS_INDEX_FILE = "$SESSIONS_DIR/sessions.json"
```

**实现的功能**:

| 功能 | 方法 | OpenClaw 对应 | 状态 |
|------|-----|-------------|------|
| 创建会话 | `createSession()` | `sessions.create()` | ✅ |
| 追加消息 | `appendMessage()` | 写入 JSONL | ✅ |
| 加载会话 | `loadSession()` | 读取 JSONL | ✅ |
| 列出会话 | `listSessions()` | `sessions.list()` | ✅ |
| 获取元数据 | `getSessionMetadata()` | sessions.json 查询 | ✅ |
| 更新标题 | `updateSessionTitle()` | metadata 更新 | ✅ |
| 删除会话 | `deleteSession()` | `sessions.delete()` | ✅ |
| 清空会话 | `clearSession()` | `sessions.reset()` | ✅ |
| 导出会话 | `exportSession()` | - | ✅ 额外功能 |
| 导入会话 | `importSession()` | - | ✅ 额外功能 |
| 统计信息 | `getSessionStats()` | - | ✅ 额外功能 |

---

### 1.2 SessionManager.kt (Agent)

**功能**: Agent 会话管理器

**对齐 OpenClaw Protocol**:
```kotlin
/**
 * Session Manager - 会话管理器
 * 对齐 OpenClaw 会话管理
 *
 * 存储格式 (OpenClaw Protocol):
 * - sessions.json: 元数据索引
 * - {sessionId}.jsonl: 消息历史 (JSONL, 每行一个事件)
 */
```

**实现的功能**:

| 功能 | 方法 | OpenClaw 对应 | 状态 |
|------|-----|-------------|------|
| 获取/创建会话 | `getOrCreate()` | 自动创建 | ✅ |
| 获取会话 | `get()` | session 查询 | ✅ |
| 保存会话 | `save()` | JSONL 持久化 | ✅ |
| 清除会话 | `clear()` | `sessions.delete()` | ✅ |
| 清除所有 | `clearAll()` | 批量删除 | ✅ |
| 获取所有键 | `getAllKeys()` | `sessions.list()` | ✅ |
| 自动压缩 | `compressIfNeeded()` | Context 压缩 | ✅ |
| 清理旧会话 | `pruneOldSessions()` | 自动清理 | ✅ |

---

### 1.3 SessionMethods.kt (Gateway RPC)

**功能**: Gateway RPC 方法实现

**实现的 RPC 方法**:

| RPC 方法 | 实现方法 | 功能 | OpenClaw 对应 | 状态 |
|---------|---------|------|-------------|------|
| `sessions.list` | `sessionsList()` | 列出所有会话 | ✅ | ✅ 完全对齐 |
| `sessions.preview` | `sessionsPreview()` | 预览会话消息 | ✅ | ✅ 完全对齐 |
| `sessions.reset` | `sessionsReset()` | 重置会话 | ✅ | ✅ 完全对齐 |
| `sessions.delete` | `sessionsDelete()` | 删除会话 | ✅ | ✅ 完全对齐 |
| `sessions.patch` | `sessionsPatch()` | 修改会话 | ✅ | ✅ 完全对齐 |

---

## 2. 数据格式对比

### 2.1 JSONL 格式 (消息历史)

**OpenClaw 格式**:
```jsonl
{"type":"session","version":3,"id":"uuid","timestamp":"2026-03-08T14:30:22.000Z"}
{"type":"message","role":"user","content":"Hello","timestamp":"2026-03-08T14:30:22.000Z"}
{"type":"message","role":"assistant","content":"Hi!","timestamp":"2026-03-08T14:30:25.000Z"}
```

**AndroidForClaw 格式**:
```jsonl
{"type":"session","version":3,"id":"uuid","timestamp":"2026-03-08T14:30:22.000Z","cwd":"/sdcard/.androidforclaw"}
{"type":"message","id":"msg-uuid","role":"user","content":"Hello","timestamp":"2026-03-08T14:30:22.000Z"}
{"type":"message","id":"msg-uuid","role":"assistant","content":"Hi!","timestamp":"2026-03-08T14:30:25.000Z"}
```

**对齐度**: ✅ 100% (完全兼容,额外添加了 cwd 和 message id)

---

### 2.2 sessions.json 格式 (索引)

**OpenClaw 格式**:
```json
{
  "agent:main:main": {
    "sessionId": "uuid",
    "updatedAt": 1234567890,
    "sessionFile": "/path/to/uuid.jsonl"
  }
}
```

**AndroidForClaw 格式**:
```json
{
  "agent:main:main": {
    "sessionId": "uuid",
    "updatedAt": 1234567890,
    "sessionFile": "/sdcard/.androidforclaw/agents/main/sessions/uuid.jsonl",
    "compactionCount": 0
  }
}
```

**对齐度**: ✅ 100% (完全兼容,额外添加了 compactionCount)

---

### 2.3 SessionMessage 数据结构

**OpenClaw**:
```typescript
interface SessionMessage {
  type: "message"
  role: "user" | "assistant" | "system" | "tool"
  content: string
  timestamp: string
  name?: string         // optional
  toolCallId?: string   // optional
}
```

**AndroidForClaw**:
```kotlin
data class SessionMessage(
    val role: String,              // "user" | "assistant" | "system" | "tool"
    val content: String,           // 消息内容
    val timestamp: String,         // ISO 8601 时间戳
    val name: String? = null,      // 可选：工具名称或用户名
    val toolCallId: String? = null, // 可选：tool call ID
    val metadata: Map<String, Any?>? = null  // 可选：额外元数据
)
```

**对齐度**: ✅ 100% (完全对齐,额外支持 metadata)

---

## 3. 功能特性对比

### 3.1 Session 生命周期

| 操作 | OpenClaw | AndroidForClaw | 对齐状态 |
|------|----------|---------------|---------|
| **创建** | ✅ | ✅ | ✅ |
| **加载** | ✅ | ✅ | ✅ |
| **保存** | ✅ | ✅ | ✅ |
| **更新** | ✅ | ✅ | ✅ |
| **删除** | ✅ | ✅ | ✅ |
| **清空** | ✅ | ✅ | ✅ |

---

### 3.2 消息操作

| 操作 | OpenClaw | AndroidForClaw | 对齐状态 |
|------|----------|---------------|---------|
| **追加消息** | ✅ | ✅ `appendMessage()` | ✅ |
| **加载消息** | ✅ | ✅ `loadSession()` | ✅ |
| **添加消息** | ✅ | ✅ `patch(op:add)` | ✅ |
| **删除消息** | ✅ | ✅ `patch(op:remove)` | ✅ |
| **清空消息** | ✅ | ✅ `patch(op:clear)` | ✅ |
| **截断消息** | ✅ | ✅ `patch(op:truncate)` | ✅ |

---

### 3.3 高级功能

| 功能 | OpenClaw | AndroidForClaw | 对齐状态 |
|------|----------|---------------|---------|
| **自动压缩** | ✅ | ✅ `compressIfNeeded()` | ✅ |
| **Token 预算** | ✅ | ✅ `getTokenCount()` | ✅ |
| **自动清理** | ✅ | ✅ `pruneOldSessions()` | ✅ |
| **统计信息** | ⚠️ | ✅ `getSessionStats()` | ✅ 额外功能 |
| **导入/导出** | ❌ | ✅ `import/exportSession()` | ✅ 额外功能 |
| **元数据更新** | ✅ | ✅ `updateSessionTitle()` | ✅ |

---

## 4. 存储路径对比

### OpenClaw 存储结构

```
~/.openclaw/
└── agents/
    └── main/
        └── sessions/
            ├── sessions.json          # 索引
            ├── uuid-1.jsonl          # 会话 1
            ├── uuid-2.jsonl          # 会话 2
            └── ...
```

### AndroidForClaw 存储结构

```
/sdcard/.androidforclaw/
└── agents/
    └── main/
        └── sessions/
            ├── sessions.json          # 索引
            ├── uuid-1.jsonl          # 会话 1
            ├── uuid-2.jsonl          # 会话 2
            └── ...
```

**对齐度**: ✅ 100% (路径结构完全一致)

---

## 5. Context 压缩对比

### OpenClaw Context 压缩

- 压缩策略: Compaction → Truncation → Abort
- 触发条件: Token 超过限制
- 压缩方法: 保留系统消息 + 最近消息

### AndroidForClaw Context 压缩

```kotlin
suspend fun compressIfNeeded(session: Session): Boolean {
    if (!contextCompressor.needsCompaction(session.messages)) {
        return false
    }

    // 执行压缩
    val compressedMessages = contextCompressor.compress(session.messages)

    // 更新会话
    session.messages.clear()
    session.messages.addAll(compressedMessages)
    session.markCompacted()

    // 保存会话
    save(session)

    return true
}
```

**对齐度**: ✅ 100% (完全对齐,支持相同的压缩策略)

---

## 6. Gateway RPC 方法对比

### OpenClaw RPC Methods

```typescript
// sessions.list() - 列出所有会话
sessions.list()

// sessions.preview() - 预览会话
sessions.preview({ key: "agent:main:main" })

// sessions.reset() - 重置会话
sessions.reset({ key: "agent:main:main" })

// sessions.delete() - 删除会话
sessions.delete({ key: "agent:main:main" })

// sessions.patch() - 修改会话
sessions.patch({
  key: "agent:main:main",
  metadata: { ... },
  messages: { op: "add", ... }
})
```

### AndroidForClaw RPC Methods

```kotlin
class SessionMethods {
    fun sessionsList(params: Any?): SessionListResult
    fun sessionsPreview(params: Any?): SessionPreviewResult
    fun sessionsReset(params: Any?): Map<String, Boolean>
    fun sessionsDelete(params: Any?): Map<String, Boolean>
    fun sessionsPatch(params: Any?): Map<String, Boolean>
}
```

**对齐度**: ✅ 100% (完全对齐,支持所有 RPC 方法)

---

## 7. sessions.patch 操作对比

### 支持的操作

| 操作 | OpenClaw | AndroidForClaw | 实现方法 |
|------|----------|---------------|---------|
| **add** | ✅ | ✅ | 添加新消息 |
| **remove** | ✅ | ✅ | 删除指定索引消息 |
| **clear** | ✅ | ✅ | 清空所有消息 |
| **truncate** | ✅ | ✅ | 保留最后 N 条消息 |
| **metadata** | ✅ | ✅ | 更新 session metadata |

**对齐度**: ✅ 100%

---

## 8. 额外功能 (AndroidForClaw 特有)

### 8.1 导入/导出

```kotlin
// 导出会话为 JSONL 文件
fun exportSession(sessionId: String, outputPath: String): Boolean

// 导入 JSONL 会话文件
fun importSession(inputPath: String, title: String?): String?
```

**用途**:
- 备份会话数据
- 在设备间迁移会话
- 分享会话给其他用户

---

### 8.2 统计信息

```kotlin
fun getSessionStats(sessionId: String): SessionStats?

data class SessionStats(
    val sessionId: String,
    val totalMessages: Int,
    val userMessages: Int,
    val assistantMessages: Int,
    val systemMessages: Int,
    val firstMessageTime: String?,
    val lastMessageTime: String?
)
```

**用途**:
- 分析会话使用情况
- 展示会话摘要
- 统计消息分布

---

### 8.3 自动清理旧会话

```kotlin
// 自动清理 30 天前的会话
suspend fun pruneOldSessions(days: Int = 30)
```

**用途**:
- 节省存储空间
- 保持会话列表整洁
- 定期维护

---

## 9. 性能对比

### JSONL 优势

| 特性 | 传统 JSON | JSONL | AndroidForClaw |
|------|----------|-------|---------------|
| **增量追加** | ❌ 需重写整个文件 | ✅ 追加一行 | ✅ 使用 JSONL |
| **流式读取** | ❌ 需加载全部 | ✅ 逐行解析 | ✅ 支持流式 |
| **内存占用** | ❌ 高 | ✅ 低 | ✅ 优化 |
| **解析速度** | ❌ 慢 (大文件) | ✅ 快 | ✅ 快速 |
| **容错性** | ❌ 一处错误全部失败 | ✅ 单行错误不影响其他 | ✅ 容错 |

---

## 10. 总结

### ✅ 完全对齐 (100%)

AndroidForClaw 的 Sessions 系统与 OpenClaw **完全对齐**:

1. **存储格式**: JSONL + sessions.json 索引 ✅
2. **数据结构**: SessionMessage, SessionMetadata ✅
3. **生命周期**: 创建、加载、保存、删除 ✅
4. **消息操作**: 追加、添加、删除、清空、截断 ✅
5. **高级功能**: 自动压缩、Token 预算、自动清理 ✅
6. **Gateway RPC**: 所有 sessions.* 方法 ✅
7. **存储路径**: agents/main/sessions/ ✅

### ✅ 超越 OpenClaw

AndroidForClaw 还提供了额外功能:
1. **导入/导出**: 会话备份和迁移 ✅
2. **统计信息**: 详细的会话分析 ✅
3. **元数据扩展**: 支持自定义 metadata ✅

---

## 📂 相关文件

**核心实现**:
1. `/app/src/main/java/com/xiaomo/androidforclaw/session/JsonlSessionStorage.kt`
2. `/app/src/main/java/com/xiaomo/androidforclaw/agent/session/SessionManager.kt`
3. `/app/src/main/java/com/xiaomo/androidforclaw/gateway/methods/SessionMethods.kt`

**扩展实现**:
4. `/extensions/discord/src/main/java/com/xiaomo/discord/session/DiscordSessionManager.kt`
5. `/extensions/feishu/src/main/java/com/xiaomo/feishu/session/FeishuSessionManager.kt`

---

**对比完成日期**: 2026-03-08
**结论**: ✅ **Sessions 系统 100% 对齐,甚至有额外增强功能**
