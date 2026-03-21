# AndroidForClaw ↔ OpenClaw 对齐报告

> 更新时间: 2026-03-20
> OpenClaw 版本: 2026.3.11 (29dc654)
> MAPPING.md 路径校验: 2026-03-20 (所有路径已对照 OpenClaw 源码矫正)

## 总体对齐度: ~78%

> 注: 此前报告为 88%,经 2026-03-20 逐文件对照 OpenClaw 源码重新评估后下调。
> 主要差距来自: Security (15%)、Channels (60%)、Sessions/Subagent 工具 (0%)。
> 核心 Agent Loop/Context/Tools 路径对齐度仍为 85-95%。

## 模块对齐详情

### 1. 常量对齐 — 95% ✅

| 常量 | OpenClaw | AndroidForClaw | 状态 |
|------|----------|---------------|------|
| SILENT_REPLY_TOKEN | "NO_REPLY" | "NO_REPLY" | ✅ |
| HEARTBEAT_TOKEN | "HEARTBEAT_OK" | "HEARTBEAT_OK" | ✅ |
| MIN_BOOTSTRAP_FILE_BUDGET_CHARS | 64 | 64 | ✅ 今日修复 (was 200) |
| DEFAULT_BOOTSTRAP_MAX_CHARS | 20000 | 20000 | ✅ |
| DEFAULT_BOOTSTRAP_TOTAL_MAX_CHARS | 150000 | 150000 | ✅ |
| BOOTSTRAP_TAIL_RATIO | 0.2 | 0.2 | ✅ |
| MAX_SKILLS_IN_PROMPT | 150 | 150 | ✅ |
| MAX_SKILLS_PROMPT_CHARS | 30000 | 30000 | ✅ |
| MAX_OVERFLOW_RECOVERY_ATTEMPTS | 3 | 3 | ✅ |
| MAX_CONSECUTIVE_ERRORS | 3 | 3 | ✅ |
| CronRetry backoffMs | [30000,60000,300000] | [30000,60000,300000] | ✅ 今日修复 (was 5 values) |
| CronFailureAlert.after | 2 | 2 | ✅ |
| CronRunLog.maxBytes | 2MB | 2MB | ✅ |
| CronRunLog.keepLines | 2000 | 2000 | ✅ |

### 2. System Prompt 22 段结构 — 85%

| # | 段落 | OpenClaw | AndroidForClaw | 状态 |
|---|------|----------|---------------|------|
| 1 | Identity | ✅ | ✅ 定制 Android 版 | ✅ |
| 2 | Tooling | 动态工具列表 | 动态工具列表 | ✅ |
| 3 | Tool Call Style | 9 行（含 exec approval） | 6 行（无 exec approval） | ⚠️ Android 无需 approval |
| 4 | Safety | 3 段宪法式 | 3 段宪法式 | ✅ 完全一致 |
| 5 | Channel Hints | 动态生成 | 动态生成 | ✅ |
| 6 | Skills (mandatory) | XML catalog + always inject | XML catalog + always inject | ✅ 今日修复 429/Retry-After |
| 7 | Memory Recall | memory_search + citations | memory_search + citations | ✅ |
| 8 | User Identity | Authorized Senders | Device Owner | ⚠️ Android 适配 |
| 9 | Current Date & Time | timezone | timezone | ✅ |
| 10 | Workspace | ~/.openclaw/workspace | /sdcard/.androidforclaw/workspace | ✅ |
| 11 | Documentation | 文档路径 | 跳过 | ⏸️ Android 不需要 |
| 12 | Workspace Files | bootstrap injection marker | bootstrap injection marker | ✅ |
| 13 | Reply Tags | [[reply_to_current]] | [[reply_to_current]] | ✅ |
| 14 | Messaging | inbound_meta.v1 JSON | inbound_meta.v1 JSON | ✅ |
| 15 | Voice (TTS) | sag/tts | 跳过 | ⏸️ |
| 16 | Group Chat Context | extraSystemPrompt | extraSystemPrompt | ✅ |
| 17 | Reactions | emoji reactions | 跳过 | ⏸️ |
| 18 | Reasoning Format | provider-specific | provider-specific | ✅ |
| 19 | Project Context | bootstrap files (8 files) | bootstrap files (8 files) | ✅ |
| 20 | Silent Replies | NO_REPLY + rules | NO_REPLY + rules | ✅ 今日修复空行 |
| 21 | Heartbeats | compact format | compact format | ✅ 今日修复 |
| 22 | Runtime | pipe-separated line | pipe-separated line | ✅ |

**已实现: 16/22 (⏸️ 跳过的 3 个不适用于 Android)**

### 3. Agent Loop — 85%

| 特性 | OpenClaw | AndroidForClaw | 状态 |
|------|----------|---------------|------|
| 最大迭代 | 40 | 40 | ✅ |
| Tool Call Dispatch | 单入口分发 | ToolCallDispatcher | ✅ |
| ToolLoopDetection | 重复检测+abort | 重复检测+abort | ✅ |
| Block Reply | text_end 中间回复 | text_end 中间回复 | ✅ |
| Context Pruning | cache-ttl soft/hard | cache-ttl soft/hard | ✅ |
| ToolResultContextGuard | 截断+compact | 截断+compact | ✅ |
| History Sanitizer | sanitize + limitTurns | sanitize + limitTurns | ✅ |
| Context Overflow Recovery | 3 次尝试 | 3 次尝试 | ✅ |
| Streaming | SSE 流式 | 非流式（batch） | ⚠️ |
| Subagent / spawn | sessions_spawn | 未实现 | ❌ |

### 4. 工具 (Tools) — 80%

| 分类 | OpenClaw 工具 | AndroidForClaw 工具 | 状态 |
|------|-------------|-------------------|------|
| 文件 | read, write, edit | read_file, write_file, edit_file, list_dir | ✅ 名称不同但功能等价 |
| 执行 | exec | exec (auto/termux/internal) | ✅ |
| 网络 | web_fetch, web_search | web_fetch, web_search | ✅ |
| 记忆 | memory_search, memory_get | memory_search, memory_get | ✅ (注册但未启用) |
| 浏览器 | browser (Playwright) | device (Playwright-aligned) | ✅ Android 适配 |
| 消息 | message | send_image | ⚠️ 部分实现 |
| 配置 | N/A (CLI) | config_get, config_set | ✅ Android 独有 |
| 会话工具 | sessions_spawn/send/list/history/yield | 未实现 | ❌ |
| 子代理 | subagents, agents_list | 未实现 | ❌ |
| 图片 | image, image_generate | 未实现 | ❌ |
| 技能商店 | skills (gateway RPC) | skills_search, skills_install | ✅ |
| PDF | pdf | 未实现 | ❌ |
| TTS | tts | 未实现 | ❌ |
| Canvas | canvas | 未实现 | ❌ |

### 5. Cron 定时任务 — 90%

| 特性 | OpenClaw | AndroidForClaw | 状态 |
|------|----------|---------------|------|
| Schedule types | at/every/cron | at/every/cron | ✅ |
| Session target | main/isolated | main/isolated | ✅ |
| Payload types | systemEvent/agentTurn | systemEvent/agentTurn | ✅ |
| Delivery | none/announce/webhook | none/announce/webhook | ✅ |
| Retry backoffMs | [30k,60k,300k] | [30k,60k,300k] | ✅ 今日修复 |
| Failure alert | after=2, cooldown=1h | after=2, cooldown=1h | ✅ |
| Run log | maxBytes=2MB | maxBytes=2MB | ✅ |
| Cron scheduler | gateway 内置 | WorkManager 实现 | ⚠️ 实现方式不同 |

### 6. Skills 系统 — 90%

| 特性 | OpenClaw | AndroidForClaw | 状态 |
|------|----------|---------------|------|
| Skill 文档格式 | YAML frontmatter + markdown | YAML frontmatter + markdown | ✅ |
| XML catalog | name + description + location | name + description + location | ✅ |
| Always skills | 全文注入 | 全文注入 | ✅ |
| Requirements check | 有 | 有 | ✅ |
| ClawHub 搜索/安装 | clawhub CLI | skills_search/skills_install | ✅ |
| 本地 skills 加载 | 工作区 + 全局 | 工作区 + assets + sdcard | ✅ |

### 7. Bootstrap 文件 — 100%

| 文件 | OpenClaw | AndroidForClaw | 状态 |
|------|----------|---------------|------|
| IDENTITY.md | ✅ | ✅ | ✅ |
| AGENTS.md | ✅ | ✅ | ✅ |
| SOUL.md | ✅ | ✅ | ✅ |
| TOOLS.md | ✅ | ✅ | ✅ |
| USER.md | ✅ | ✅ | ✅ |
| HEARTBEAT.md | ✅ | ✅ | ✅ |
| BOOTSTRAP.md | ✅ | ✅ | ✅ |
| MEMORY.md | ✅ | ✅ | ✅ |
| Budget (per-file) | 20000 chars | 20000 chars | ✅ |
| Budget (total) | 150000 chars | 150000 chars | ✅ |
| Truncation | head 80% + tail 20% | head 80% + tail 20% | ✅ |

### 8. Context 管理 — 85%

| 特性 | OpenClaw | AndroidForClaw | 状态 |
|------|----------|---------------|------|
| Context window guard | warn/block thresholds | warn/block thresholds | ✅ |
| Soft trim ratio | 0.3 | 0.3 | ✅ |
| Hard clear ratio | 0.5 | 0.5 | ✅ |
| Min prunable chars | 50000 | 50000 | ✅ |
| Keep last assistants | 3 | 3 | ✅ |
| Soft trim head/tail | 1500/1500 | 1500/1500 | ✅ |
| Compaction mode | safeguard | 未实现 | ⚠️ |
| Context pruning mode | cache-ttl | cache-ttl（简化版） | ⚠️ |

### 9. 渠道 (Channels) — 60%

| 渠道 | OpenClaw | AndroidForClaw | 状态 |
|------|----------|---------------|------|
| Feishu | ✅ 完整插件 | ✅ 扩展模块 | ✅ |
| Discord | ✅ | ✅ 扩展模块 | ✅ |
| Telegram | ✅ | ✅ 扩展模块 | ✅ |
| WhatsApp | ✅ | ✅ 扩展模块 | ✅ |
| Signal | ✅ | ✅ 扩展模块 | ✅ |
| Slack | ✅ | ✅ 扩展模块 | ✅ |
| iMessage | ✅ | ❌ | ❌ iOS only |
| IRC | ✅ | ❌ | ❌ |
| LINE | ✅ | ❌ | ❌ |
| Web | ✅ | ❌ Android 本地 | ⚠️ |

---

## 今日修复记录 (2026-03-15)

| # | 模块 | 问题 | 修复 |
|---|------|------|------|
| 1 | Constants | MIN_BOOTSTRAP_FILE_BUDGET_CHARS = 200 | → 64 |
| 2 | Heartbeat | 多余 Examples 块 | 删除，compact 格式 |
| 3 | Heartbeat | 读 HEARTBEAT.md 首行作 prompt | → "(configured)" |
| 4 | Skills | 缺少 429/Retry-After | 补齐 |
| 5 | Silent Replies | 段落间缺空行 | 对齐格式 |
| 6 | CronTypes | backoffMs 5 个值 | → 3 个值 |

## 未对齐项（待做）

| 优先级 | 模块 | 说明 | OpenClaw 源文件 |
|--------|------|------|----------------|
| **P0** | Security | Pairing 配对机制 | `src/pairing/` |
| **P0** | Security | External Content 包装 | `src/security/external-content.ts` |
| **P0** | Security | DM Policy | `src/security/dm-policy-shared.ts` |
| P1 | Agent Loop | 流式响应 (SSE streaming) | `src/agents/pi-embedded-subscribe.ts` |
| P1 | Tools | message 工具完善 | `src/agents/tools/message-tool.ts` |
| P1 | Tools | Sessions 工具族 (spawn/send/list/history/yield) | `src/agents/tools/sessions-*.ts` |
| P2 | Agent Loop | Subagent 体系 | `src/agents/subagent-*.ts` |
| P2 | Context | compaction safeguard 模式 | `src/agents/compaction.ts` |
| P2 | Infra | Context Engine 注册/委托 | `src/context-engine/` |
| P3 | Tools | PDF/TTS/Canvas/Image | `src/agents/tools/{pdf,tts,canvas,image}-tool.ts` |
| P3 | Channels | iMessage/IRC/LINE | - |

## 源码参考路径勘误 (2026-03-20)

MAPPING.md 中大量 OpenClaw 路径在 2026-03-20 经对照修正:
- `src/commands/*.ts` → 实际为 `src/agents/tools/*.ts` 或 `src/agents/bash-tools.*.ts`
- `src/agents/run-agent-loop.ts` → `src/agents/agent-command.ts`
- `src/agents/tool-registry.ts` → `src/agents/tool-catalog.ts`
- `src/agents/skills-loader.ts` → `src/agents/skills.ts`
- `src/agents/build-context.ts` → `src/agents/context.ts`
- `src/gateway/gateway-server.ts` → `src/gateway/server.ts`
- `src/config/config-loader.ts` → `src/config/io.ts`
- `src/memory/memory-manager.ts` → `src/memory/manager.ts`
- 详见 [MAPPING.md](../MAPPING.md)
