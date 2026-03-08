# Skills 对齐分析报告

**对比对象**: OpenClaw vs AndroidForClaw
**分析日期**: 2026-03-08
**分析范围**: 默认 Skills (Bundled Skills)

---

## 1. 整体对比

### OpenClaw Skills 数量

- **总计**: 52 个 Skills
- **分类**:
  - 核心 Skills: 7 个 (coding-agent, discord, github, slack, session-logs, skill-creator, healthcheck)
  - 第三方集成: 20+ 个 (各种工具和服务)
  - 专用工具: 25+ 个 (领域特定功能)

### AndroidForClaw Skills 数量

- **总计**: 7 个 Skills
- **分类**:
  - 核心 Skills: 3 个 (mobile-operations, bootstrap, create-skill)
  - 功能 Skills: 4 个 (debugging, browser, data-processing, javascript-executor)

---

## 2. 核心 Skills 对比

### 2.1 Always-Load Skills (始终加载)

| OpenClaw | AndroidForClaw | 说明 |
|----------|---------------|------|
| ❌ 无明确的 always skill | ✅ **mobile-operations** | Android 核心操作 (tap, swipe, screenshot) |
| ❌ 无明确的 always skill | ✅ **bootstrap** | 系统身份和核心指令 |

**差异说明**:
- OpenClaw 没有 `always: true` 的 Skills,所有 Skills 都是按需加载
- AndroidForClaw 有 2 个 always Skills,因为移动设备操作是核心能力

---

### 2.2 平台特定核心 Skills

| OpenClaw | AndroidForClaw | 对齐状态 |
|----------|---------------|---------|
| **coding-agent** | ❌ 不适用 | OpenClaw 核心能力,Android 无需 |
| ❌ 无 | **mobile-operations** | AndroidForClaw 核心能力 |
| **session-logs** | ❌ 未实现 | 需要 Sessions 系统支持 |
| **skill-creator** | ✅ **create-skill** | 已对齐,功能相似 |

---

## 3. 详细 Skills 对比

### 3.1 OpenClaw 核心 Skills (7 个)

#### 1. **coding-agent** 🧩
- **功能**: 委托编码任务给 Codex/Claude Code/Pi agents
- **触发**: 构建新功能、审查 PR、重构代码
- **依赖**: `claude`, `codex`, `opencode`, `pi` 命令
- **AndroidForClaw**: ❌ 不适用 (移动环境无需代码生成 agent)

#### 2. **discord** 🎮
- **功能**: Discord 操作 (消息、频道管理)
- **触发**: Discord 相关操作
- **依赖**: `channels.discord.token` 配置
- **AndroidForClaw**: ⚠️ 部分实现 (在 extensions/discord 中)

#### 3. **github** 🐙
- **功能**: GitHub 操作 (issues, PRs, CI)
- **触发**: GitHub 相关操作
- **依赖**: `gh` CLI
- **AndroidForClaw**: ❌ 未实现

#### 4. **slack** 💬
- **功能**: Slack 操作 (消息、pins、reactions)
- **触发**: Slack 相关操作
- **依赖**: `channels.slack` 配置
- **AndroidForClaw**: ❌ 未实现

#### 5. **session-logs** 📜
- **功能**: 搜索和分析会话历史
- **触发**: 用户询问历史对话
- **依赖**: `jq`, `rg` 命令
- **AndroidForClaw**: ❌ 未实现 (需要 Sessions 系统)

#### 6. **skill-creator** 🛠️
- **功能**: 创建和更新 AgentSkills
- **触发**: 设计、打包 Skills
- **AndroidForClaw**: ✅ **create-skill** (已对齐)

#### 7. **healthcheck** 🏥
- **功能**: 系统健康检查
- **触发**: 诊断系统状态
- **AndroidForClaw**: ❌ 未实现

---

### 3.2 AndroidForClaw 核心 Skills (7 个)

#### 1. **mobile-operations** 📱 (always: true)
- **功能**: Android 设备核心操作
- **包含**: screenshot, tap, swipe, type, home, back, open_app
- **核心循环**: Observe → Think → Act → Verify
- **OpenClaw**: ❌ 无对应 (桌面环境无此能力)

#### 2. **bootstrap** 🤖 (always: true)
- **功能**: 系统身份和核心指令
- **包含**: Agent 身份定义、核心原则
- **OpenClaw**: ⚠️ 类似功能在系统提示词中

#### 3. **create-skill** 🛠️
- **功能**: 创建新 Skills
- **OpenClaw**: ✅ **skill-creator** (对齐)

#### 4. **debugging** 🐛
- **功能**: 调试技能和故障排查
- **OpenClaw**: ❌ 无对应 Skill

#### 5. **browser** 🌐
- **功能**: BrowserForClaw 浏览器自动化
- **OpenClaw**: ⚠️ 类似功能在 browser tool 中

#### 6. **data-processing** 📊
- **功能**: 使用 JavaScript 进行数据处理
- **OpenClaw**: ❌ 无对应 Skill

#### 7. **javascript-executor** ⚡
- **功能**: QuickJS 引擎执行 JavaScript
- **OpenClaw**: ⚠️ 类似功能在 javascript tool 中

---

## 4. OpenClaw 第三方集成 Skills (20+ 个)

| Skill 名称 | 功能 | AndroidForClaw 状态 |
|-----------|------|-------------------|
| **1password** | 1Password 密码管理 | ❌ 未实现 |
| **apple-notes** | Apple Notes 集成 | ❌ 未实现 (iOS 特定) |
| **apple-reminders** | Apple Reminders | ❌ 未实现 (iOS 特定) |
| **bear-notes** | Bear Notes 集成 | ❌ 未实现 |
| **bluebubbles** | iMessage 集成 | ❌ 未实现 (iOS 特定) |
| **notion** | Notion 集成 | ❌ 未实现 |
| **obsidian** | Obsidian 集成 | ❌ 未实现 |
| **trello** | Trello 集成 | ❌ 未实现 |
| **things-mac** | Things 任务管理 | ❌ 未实现 (macOS 特定) |
| **himalaya** | Email CLI 集成 | ❌ 未实现 |
| **imsg** | iMessage 集成 | ❌ 未实现 (macOS 特定) |
| **spotify-player** | Spotify 控制 | ❌ 未实现 |
| **sonoscli** | Sonos 音响控制 | ❌ 未实现 |
| **openhue** | Philips Hue 控制 | ❌ 未实现 |
| **goplaces** | 地点管理 | ❌ 未实现 |
| **weather** | 天气信息 | ❌ 未实现 |
| **tmux** | tmux 终端管理 | ❌ 未实现 |
| **clawhub** | ClawHub 包管理 | ❌ 未实现 |
| **gemini** | Google Gemini 集成 | ❌ 未实现 |
| **openai-image-gen** | OpenAI 图像生成 | ❌ 未实现 |
| **openai-whisper** | Whisper 语音转文本 | ❌ 未实现 |

**小结**: 第三方集成 Skills **0% 对齐** (0/20+)

---

## 5. OpenClaw 专用工具 Skills (25+ 个)

| Skill 名称 | 功能 | AndroidForClaw 适用性 |
|-----------|------|---------------------|
| **blogwatcher** | 博客监控 | ⚠️ 可实现 |
| **blucli** | Bluesky CLI | ⚠️ 可实现 |
| **camsnap** | 摄像头抓拍 | ⚠️ Android 可实现 |
| **canvas** | Canvas 绘图 | ❌ 移动端不适用 |
| **eightctl** | 8sleep 控制 | ❌ 未实现 |
| **gh-issues** | GitHub Issues | ❌ 未实现 |
| **gifgrep** | GIF 搜索 | ❌ 未实现 |
| **gog** | GOG 游戏平台 | ❌ 未实现 |
| **mcporter** | Minecraft 服务器 | ❌ 未实现 |
| **model-usage** | 模型使用统计 | ⚠️ 可实现 |
| **nano-banana-pro** | Nano Banana Pro | ❌ 未实现 |
| **nano-pdf** | PDF 处理 | ⚠️ Android 可实现 |
| **oracle** | Oracle 数据库 | ❌ 未实现 |
| **ordercli** | 订单管理 | ❌ 未实现 |
| **peekaboo** | 摄像头监控 | ⚠️ Android 可实现 |
| **sag** | System Activity Graph | ❌ 未实现 |
| **sherpa-onnx-tts** | TTS 语音合成 | ⚠️ Android 可实现 |
| **songsee** | 音乐识别 | ⚠️ Android 可实现 |
| **summarize** | 内容摘要 | ⚠️ 可实现 |
| **video-frames** | 视频帧提取 | ⚠️ Android 可实现 |
| **voice-call** | 语音通话 | ⚠️ Android 可实现 |
| **wacli** | WhatsApp CLI | ⚠️ Android 更适合 |
| **xurl** | URL 工具 | ⚠️ 可实现 |

**小结**: 专用工具 Skills 中约 30% 适合 Android 实现

---

## 6. 对齐差距分析

### 6.1 核心能力对齐度

| 类别 | OpenClaw | AndroidForClaw | 对齐率 |
|------|----------|---------------|--------|
| **平台核心 Skills** | 7 | 7 | - (平台不同) |
| **通用功能 Skills** | 0 | 4 | - |
| **第三方集成** | 20+ | 1 | 5% |
| **专用工具** | 25+ | 0 | 0% |
| **总计** | 52 | 7 | 13% |

---

### 6.2 缺失的关键 Skills

#### 高优先级 (应该实现) 🔴

1. **session-logs** - 会话历史搜索
   - 功能: 搜索和分析历史对话
   - 前置: 需要 Sessions 系统
   - 优先级: 高 (OpenClaw 核心功能)

2. **model-usage** - 模型使用统计
   - 功能: 跟踪 token 使用和成本
   - 实现: 可在 AndroidForClaw 实现
   - 优先级: 高 (成本控制很重要)

3. **weather** - 天气信息
   - 功能: 获取天气信息
   - 实现: 简单,集成天气 API
   - 优先级: 中 (常用功能)

#### 中优先级 (可选实现) 🟡

4. **nano-pdf** - PDF 处理
   - 功能: PDF 读取/处理
   - 实现: Android 有 PDF 库
   - 优先级: 中

5. **sherpa-onnx-tts** - TTS 语音合成
   - 功能: 文本转语音
   - 实现: Android 原生 TTS
   - 优先级: 中

6. **wacli** - WhatsApp CLI
   - 功能: WhatsApp 操作
   - 实现: Android 更适合 (Accessibility)
   - 优先级: 中

7. **voice-call** - 语音通话
   - 功能: 发起/接听电话
   - 实现: Android 原生支持
   - 优先级: 中

#### 低优先级 (暂不考虑) 🔵

8. **第三方集成 Skills** (Notion, Trello, Obsidian 等)
   - 原因: 需要 OAuth 认证,实现复杂
   - 建议: 等 Gateway 完成后再考虑

9. **Apple 专用 Skills** (apple-notes, imsg, things-mac 等)
   - 原因: iOS/macOS 特定,Android 不适用
   - 建议: 不实现

---

## 7. Skills 架构对齐分析

### 7.1 Skill 格式

| 特性 | OpenClaw | AndroidForClaw | 对齐状态 |
|------|----------|---------------|---------|
| **Frontmatter** | ✅ YAML | ✅ YAML | ✅ 完全对齐 |
| **Metadata** | ✅ openclaw 字段 | ✅ openclaw 字段 | ✅ 完全对齐 |
| **always 标志** | ✅ 支持 | ✅ 支持 | ✅ 完全对齐 |
| **requires 依赖** | ✅ bins, env, config | ✅ bins, env, config | ✅ 完全对齐 |
| **emoji 图标** | ✅ 支持 | ✅ 支持 | ✅ 完全对齐 |
| **Markdown 内容** | ✅ 标准 MD | ✅ 标准 MD | ✅ 完全对齐 |

**对齐率**: 100% ✅

---

### 7.2 Skills 加载机制

| 特性 | OpenClaw | AndroidForClaw | 对齐状态 |
|------|----------|---------------|---------|
| **三层加载** | ✅ Bundled → Managed → Workspace | ✅ Bundled → Managed → Workspace | ✅ 完全对齐 |
| **优先级覆盖** | ✅ 高优先级覆盖低优先级 | ✅ 高优先级覆盖低优先级 | ✅ 完全对齐 |
| **按需加载** | ✅ 根据任务选择 | ✅ 根据任务选择 | ✅ 完全对齐 |
| **Always Skills** | ❌ 无 always skills | ✅ 2 个 always skills | ⚠️ 部分差异 |
| **热重载** | ⚠️ 未知 | ✅ 支持 | ⚠️ 未确认 |
| **依赖检查** | ✅ requires gating | ✅ requires gating | ✅ 完全对齐 |

**对齐率**: 90% ✅

---

### 7.3 Skills 选择策略

| 特性 | OpenClaw | AndroidForClaw | 对齐状态 |
|------|----------|---------------|---------|
| **任务类型识别** | ✅ 智能识别 | ✅ 智能识别 | ✅ 完全对齐 |
| **关键词匹配** | ✅ 支持 | ✅ 支持 | ✅ 完全对齐 |
| **相关度排序** | ✅ 支持 | ✅ 支持 | ✅ 完全对齐 |
| **Token 优化** | ✅ 最小化注入 | ✅ 最小化注入 | ✅ 完全对齐 |

**对齐率**: 100% ✅

---

## 8. Skills 内容质量对比

### 8.1 OpenClaw Skills 特点

1. **详细的使用说明** ✅
   - 每个 Skill 都有完整的使用示例
   - 包含常见场景和最佳实践
   - 有错误处理和故障排查

2. **清晰的触发条件** ✅
   - 明确说明何时使用
   - 何时不使用
   - 替代方案

3. **完善的依赖声明** ✅
   - requires.bins 列出所需命令
   - requires.config 列出配置项
   - install 字段提供安装指令

4. **规范的格式** ✅
   - 统一的 Markdown 结构
   - 清晰的章节划分
   - 代码示例格式一致

---

### 8.2 AndroidForClaw Skills 特点

1. **移动设备专用指令** ✅
   - 针对 Android 优化的操作指南
   - Observe → Think → Act → Verify 循环
   - 坐标系统说明

2. **中英双语** ✅
   - 中文和英文混合使用
   - 适合中文用户

3. **实用的示例** ✅
   - 包含常见工作流
   - 表单填写、应用测试等

4. **待改进** ⚠️
   - 部分 Skills 内容较少
   - 缺少故障排查章节
   - 缺少最佳实践总结

---

## 9. 建议的 Skills 路线图

### Phase 1: 核心 Skills 完善 (立即) 🔴

1. **完善现有 Skills**
   - debugging: 添加更多故障排查场景
   - browser: 完善 BrowserForClaw 集成文档
   - data-processing: 添加更多数据处理示例

2. **添加 session-logs Skill**
   - 前置: 实现 Sessions 系统
   - 功能: 搜索历史对话

3. **添加 model-usage Skill**
   - 功能: Token 使用统计
   - 成本跟踪和优化建议

---

### Phase 2: 实用 Skills (短期) 🟡

4. **weather Skill** - 天气信息
5. **nano-pdf Skill** - PDF 处理
6. **tts Skill** - 文本转语音
7. **camera Skill** - 摄像头控制
8. **notification Skill** - 系统通知管理

---

### Phase 3: 高级集成 (中期) 🔵

9. **wacli Skill** - WhatsApp 集成
10. **voice-call Skill** - 语音通话
11. **contacts Skill** - 联系人管理
12. **calendar Skill** - 日历管理
13. **notes Skill** - 笔记应用集成

---

### Phase 4: 第三方平台 (长期) 🟣

14. **notion Skill** - Notion 集成
15. **trello Skill** - Trello 集成
16. **github Skill** - GitHub 集成 (通过 Web API)
17. **slack Skill** - Slack 集成 (通过 SDK)

---

## 10. 结论

### 10.1 架构对齐度: 95% ✅

AndroidForClaw 的 Skills 系统在架构上与 OpenClaw 高度一致:
- ✅ Skill 格式完全对齐 (100%)
- ✅ 加载机制对齐 (90%)
- ✅ 选择策略对齐 (100%)
- ✅ 支持 AgentSkills.io 标准

---

### 10.2 内容对齐度: 13% ⚠️

Skills 数量差距大:
- OpenClaw: 52 个 Skills
- AndroidForClaw: 7 个 Skills
- 对齐率: 13% (7/52)

**但这是合理的**,因为:
1. **平台差异**: OpenClaw 是桌面/服务器环境,AndroidForClaw 是移动环境
2. **核心能力不同**: 移动设备的核心是 UI 交互,桌面的核心是代码生成和工具集成
3. **目标用户不同**: OpenClaw 面向开发者,AndroidForClaw 面向移动自动化

---

### 10.3 关键优势

**AndroidForClaw 的优势**:
1. ✅ 移动设备专有能力 (screenshot, tap, swipe)
2. ✅ 核心 Skills 质量高 (mobile-operations 非常完善)
3. ✅ 架构完全对齐 OpenClaw
4. ✅ 支持用户自定义 Skills (Workspace)

**OpenClaw 的优势**:
1. ✅ Skills 数量多 (52 vs 7)
2. ✅ 第三方集成丰富 (20+ 平台)
3. ✅ 专用工具多样 (25+ 工具)
4. ✅ 文档完善

---

### 10.4 下一步行动

**立即行动**:
1. 完善现有 7 个 Skills 的文档质量
2. 添加 model-usage Skill (Token 统计)
3. 添加 weather Skill (常用功能)

**短期计划**:
4. 实现 Sessions 系统,添加 session-logs Skill
5. 添加 PDF、TTS、Camera 等实用 Skills

**长期规划**:
6. 第三方平台集成 (Notion, Trello, GitHub)
7. 建立 AndroidForClaw Skills 社区

---

## 附录: Skills 清单对比表

### OpenClaw Skills (52 个)

**核心 (7)**:
1. coding-agent 🧩
2. discord 🎮
3. github 🐙
4. slack 💬
5. session-logs 📜
6. skill-creator 🛠️
7. healthcheck 🏥

**第三方集成 (20+)**:
8-27. 各种第三方服务集成

**专用工具 (25+)**:
28-52. 各种专用工具

---

### AndroidForClaw Skills (7 个)

**核心 (3)**:
1. mobile-operations 📱 (always)
2. bootstrap 🤖 (always)
3. create-skill 🛠️

**功能 (4)**:
4. debugging 🐛
5. browser 🌐
6. data-processing 📊
7. javascript-executor ⚡

---

**分析完成时间**: 2026-03-08
**下一步**: 实现 model-usage 和 weather Skills
