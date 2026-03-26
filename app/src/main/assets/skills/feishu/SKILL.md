---
name: feishu
description: |
  Feishu messaging and media operations. Activate when user asks to send messages, images, or media through Feishu.
---

# Feishu Messaging Skill

Guidance on sending messages and images through Feishu from AndroidForClaw.

## Sending Images

The system **automatically detects** screenshot/image paths in your response and uploads them to Feishu.

### How It Works

1. Use `screenshot` or `eye.snap` tool to capture an image
2. Include the returned path in your response
3. System auto-detects the path, uploads the image, and sends it

### Path Formats Supported

- File paths: `路径: /storage/emulated/0/...`
- Content URIs: `路径: content://com.xiaomo.androidforclaw.accessibility.fileprovider/...`

### Rules

- ✅ Include the full `路径: ...` line in your response
- ✅ Use the exact path format from the tool result
- ❌ Don't manually call upload or send image functions
- ❌ Don't modify or remove the path

The path is automatically removed from the text message after the image is sent.

## Sending Text

Respond normally. Your response is automatically sent to Feishu.

### Markdown Support

Feishu renders: code blocks, tables, bold, italic, links, etc. Use proper Markdown for readability.

## Message Flow

```
User Message (Feishu) → Agent Processing → Agent Response
  → Auto-detect image paths → Upload & Send Image
  → Remaining text → Send as Text Message
```
