---
name: feishu-urgent
description: |
  Feishu urgent notification tool. Activate when user wants to send urgent messages or critical alerts.
---

# Feishu Urgent Tool

Tool `feishu_urgent` for sending urgent notifications.

## Actions

### Send Urgent Message

```json
{
  "action": "send",
  "user_id": "ou_xxx",
  "title": "Urgent: Production Issue",
  "content": "Service is down, please check immediately!"
}
```

Sends an urgent notification with push notification to user's devices.

### Send Urgent to Multiple Users

```json
{
  "action": "send_batch",
  "user_ids": ["ou_xxx", "ou_yyy"],
  "title": "Critical Alert",
  "content": "System maintenance starting in 5 minutes"
}
```

## Notification Features

- **Push Notification**: System push to all user devices
- **Priority Display**: Shows as urgent in Feishu app
- **Sound Alert**: Plays notification sound even in DND mode

## Best Practices

1. **Use sparingly** - only for truly urgent matters
2. **Clear titles** - make urgency clear in title
3. **Action items** - include what needs to be done
4. **Avoid spam** - don't overuse to maintain effectiveness

## Permissions

Required: `im:message:send_as_bot`, `im:message`
