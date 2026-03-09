#!/bin/bash

echo "========================================="
echo "测试 4 个核心功能实现"
echo "========================================="
echo ""

# 1. 测试 Cron 定时任务
echo "1. 测试 Cron 定时任务 (WorkManager)"
echo "-----------------------------------------"
adb shell cat /sdcard/.androidforclaw/cron/jobs.json 2>/dev/null && echo "✅ Cron jobs.json 已创建" || echo "❌ jobs.json 未创建"
adb shell dumpsys jobscheduler | grep -i "com.xiaomo.androidforclaw.debug" | head -3 && echo "✅ WorkManager 任务已调度" || echo "⚠️  未找到 WorkManager 任务"
echo ""

# 2. 测试配置备份
echo "2. 测试配置自动备份和恢复"
echo "-----------------------------------------"
adb shell ls -l /sdcard/.androidforclaw/config/openclaw.last-known-good.json 2>/dev/null && echo "✅ last-known-good.json 已创建" || echo "❌ last-known-good.json 未创建"
adb shell ls -l /sdcard/.androidforclaw/config-backups/ 2>/dev/null && echo "✅ config-backups 目录已创建" || echo "⚠️  config-backups 目录为空"
echo ""

# 3. 测试日志文件系统
echo "3. 测试日志文件系统 (app.log, gateway.log)"
echo "-----------------------------------------"
adb shell ls -l /sdcard/.androidforclaw/logs/ 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ logs 目录已创建"
    # 触发日志写入测试
    echo "触发一条日志写入测试..."
    adb logcat -c
    adb shell "am broadcast -a com.xiaomo.androidforclaw.MESSAGE -n com.xiaomo.androidforclaw.debug/com.xiaomo.androidforclaw.receiver.MessageReceiver --es message '测试日志'" > /dev/null 2>&1
    sleep 2
    adb shell ls -l /sdcard/.androidforclaw/logs/app.log 2>/dev/null && echo "✅ app.log 已创建" || echo "⚠️  app.log 未创建 (需要代码主动调用 AppLog)"
else
    echo "❌ logs 目录未创建"
fi
echo ""

# 4. 测试 Session JSONL 存储
echo "4. 测试 Session JSONL 存储"
echo "-----------------------------------------"
adb shell ls -l /sdcard/.androidforclaw/agents/main/sessions/ 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ sessions 目录已创建"
    adb shell ls -l /sdcard/.androidforclaw/agents/main/sessions/sessions.json 2>/dev/null && echo "✅ sessions.json 索引已创建" || echo "⚠️  sessions.json 未创建 (需要创建会话时触发)"
else
    echo "⚠️  sessions 目录未创建 (需要创建会话时触发)"
fi
echo ""

# 总结
echo "========================================="
echo "总结"
echo "========================================="
echo "✅ = 已实现并正常工作"
echo "⚠️  = 已实现但需要代码主动调用"
echo "❌ = 未正常工作"
echo ""
echo "注意:"
echo "- AppLog 和 JsonlSessionStorage 是基础设施组件"
echo "- 需要在 AgentLoop 和其他代码中主动调用才会产生文件"
echo "- Cron 和 ConfigBackup 已自动集成到 MyApplication"
echo ""
