#!/bin/bash

# OpenRouter 配置设置脚本
#
# 使用方法:
# 1. 设置环境变量: export OPENROUTER_API_KEY="your-key-here"
# 2. 运行此脚本: ./setup_openrouter.sh

echo "========================================="
echo "OpenRouter 配置设置"
echo "========================================="
echo ""

# 检查环境变量
if [ -z "$OPENROUTER_API_KEY" ]; then
    echo "❌ 错误: OPENROUTER_API_KEY 环境变量未设置"
    echo ""
    echo "请先设置环境变量:"
    echo "  export OPENROUTER_API_KEY=\"your-api-key-here\""
    echo ""
    echo "获取 API Key:"
    echo "  1. 访问 https://openrouter.ai/"
    echo "  2. 登录并获取 API Key"
    echo "  3. 复制 API Key 并设置为环境变量"
    exit 1
fi

echo "✅ 找到 OPENROUTER_API_KEY"
echo ""

# 创建配置文件
echo "📝 创建配置文件..."
cat > /tmp/openclaw_openrouter.json << EOF
{
  "models": {
    "providers": {
      "openrouter": {
        "baseUrl": "https://openrouter.ai/api/v1",
        "apiKey": "$OPENROUTER_API_KEY",
        "api": "openai-completions",
        "authHeader": true,
        "models": [
          {
            "id": "anthropic/claude-opus-4-6",
            "name": "Claude Opus 4.6",
            "reasoning": false,
            "input": ["text", "image"],
            "contextWindow": 200000,
            "maxTokens": 8192
          },
          {
            "id": "anthropic/claude-sonnet-4-6",
            "name": "Claude Sonnet 4.6",
            "reasoning": false,
            "input": ["text", "image"],
            "contextWindow": 200000,
            "maxTokens": 8192
          }
        ]
      }
    }
  },
  "agents": {
    "defaults": {
      "model": {
        "primary": "openrouter/anthropic/claude-opus-4-6"
      }
    }
  },
  "agent": {
    "maxIterations": 40,
    "timeout": 300000
  },
  "gateway": {
    "enabled": true,
    "port": 8080,
    "host": "0.0.0.0"
  },
  "ui": {
    "floatingWindow": {
      "enabled": true,
      "showProgress": true
    }
  },
  "logging": {
    "level": "INFO",
    "logToFile": true
  }
}
EOF

echo "✅ 配置文件已创建"
echo ""

# 推送到设备
echo "📤 推送配置到设备..."
adb push /tmp/openclaw_openrouter.json /sdcard/.androidforclaw/openclaw.json

if [ $? -eq 0 ]; then
    echo "✅ 配置已推送到设备"
    echo ""
    echo "📋 验证配置..."
    adb shell cat /sdcard/.androidforclaw/openclaw.json | grep -E "baseUrl|primary" | head -5
    echo ""
    echo "========================================="
    echo "✅ OpenRouter 配置完成!"
    echo "========================================="
    echo ""
    echo "下一步:"
    echo "  1. 重启应用: adb shell am force-stop com.xiaomo.androidforclaw.debug"
    echo "  2. 测试消息: adb shell am broadcast -a com.xiaomo.androidforclaw.ACTION_EXECUTE_AGENT \\"
    echo "               -n com.xiaomo.androidforclaw.debug/com.xiaomo.androidforclaw.core.AgentMessageReceiver \\"
    echo "               --es message \"截个图\""
else
    echo "❌ 配置推送失败"
    exit 1
fi
