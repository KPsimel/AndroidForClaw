#!/bin/bash

# BrowserForClaw MCP Bridge Setup Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🚀 Setting up BrowserForClaw MCP Bridge..."
echo ""

# Step 1: Install Node dependencies
echo "📦 Installing Node.js dependencies..."
cd "$SCRIPT_DIR"
npm install
echo "✅ Dependencies installed"
echo ""

# Step 2: Check Android device
echo "📱 Checking Android device..."
if ! command -v adb &> /dev/null; then
    echo "❌ adb not found. Please install Android SDK Platform Tools."
    exit 1
fi

DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo "⚠️  No Android device connected."
    echo "   Please connect your device and enable USB debugging."
    exit 1
fi
echo "✅ Android device connected"
echo ""

# Step 3: Setup port forwarding
echo "🔌 Setting up port forwarding..."
adb forward tcp:58765 tcp:58765
echo "✅ Port forwarding: tcp:58765 -> tcp:58765"
echo ""

# Step 4: Check BrowserForClaw
echo "🌐 Checking BrowserForClaw..."
if ! adb shell ps | grep -q "info.plateaukao.einkbro"; then
    echo "⚠️  BrowserForClaw not running. Starting..."
    adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity
    sleep 2
fi
echo "✅ BrowserForClaw running"
echo ""

# Step 5: Test connection
echo "🧪 Testing connection..."
if curl -s -f http://localhost:58765/health > /dev/null; then
    echo "✅ BrowserForClaw API is responding"
    echo ""
else
    echo "❌ Cannot connect to BrowserForClaw API"
    echo "   Please check if the app is running and port forwarding is correct."
    exit 1
fi

# Step 6: Print Claude Desktop config
echo "📝 Claude Desktop Configuration:"
echo ""
echo "Add this to your Claude Desktop config file:"
echo ""
echo "  macOS: ~/Library/Application Support/Claude/claude_desktop_config.json"
echo "  Windows: %APPDATA%\\Claude\\claude_desktop_config.json"
echo "  Linux: ~/.config/Claude/claude_desktop_config.json"
echo ""
echo "{"
echo "  \"mcpServers\": {"
echo "    \"browserforclaw\": {"
echo "      \"command\": \"node\","
echo "      \"args\": ["
echo "        \"$SCRIPT_DIR/index.js\""
echo "      ],"
echo "      \"env\": {"
echo "        \"BROWSER_API_URL\": \"http://localhost:58765\","
echo "        \"LOG_LEVEL\": \"info\""
echo "      }"
echo "    }"
echo "  }"
echo "}"
echo ""
echo "✅ Setup complete! Restart Claude Desktop to use BrowserForClaw tools."
