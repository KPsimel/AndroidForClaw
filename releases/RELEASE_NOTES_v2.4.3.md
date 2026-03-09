# v2.4.3 - ClawHub 集成 & Browser Tool 修复

## 🎉 新功能

### ClawHub 完整支持
- ✅ **skills.search** - 搜索 ClawHub 技能
- ✅ **skills.install** - 安装 ClawHub 技能
- ✅ **skills.status** - 查询技能状态
- 📚 添加完整的 [ClawHub 集成指南](../CLAWHUB_GUIDE.md)

**重要**: ClawHub API 完全可用 (clawhub.ai),即使网站 (clawhub.com) 显示 404

### Browser Tool 改进
- 🌐 修正 BrowserForClaw 端口配置 (8765)
- 📖 browser skill 添加百度搜索完整示例
- ⚠️ 明确提示:不要用 open_app 打开浏览器,直接用 browser tool

## 🔧 技术改进

### 签名配置统一
- 🔐 使用统一的 keystore.jks 签名
- ✅ Release 版本自动签名
- 📦 两个 APK 使用相同签名

### UI 优化
- 🎨 ConfigActivity Skills 页面列表显示改进
- 💬 ChatScreen 和 SkillsActivity UI 优化

## 📦 下载

### 主应用
- **androidforclaw-v2.4.3-release.apk** (31MB)
  - 包含完整功能
  - 需要 Android 8.0+ (API 26+)

### 无障碍服务
- **androidforclaw-accessibility-v2.4.3-release.apk** (4.3MB)
  - 独立的无障碍服务 APK
  - 提供高级权限管理

**安装说明**: 两个 APK 都需要安装才能使用完整功能

## 🔗 相关文档

- [ClawHub 集成指南](../CLAWHUB_GUIDE.md) - ClawHub 使用详解
- [CLAUDE.md](../CLAUDE.md) - 项目开发指南
- [ARCHITECTURE.md](../ARCHITECTURE.md) - 架构设计文档

## 🙏 致谢

感谢 [OpenClaw](https://github.com/openclaw/openclaw) 提供的架构设计和 ClawHub API 支持。

---

**完整更新日志**: https://github.com/xiaomochn/AndroidForClaw/compare/v2.4.2...v2.4.3
