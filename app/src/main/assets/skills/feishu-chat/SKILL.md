---
name: feishu-chat
description: |
  Feishu chat management. Activate when user wants to create groups, manage chat members, or get chat info.
---

# Feishu Chat Tool

Tool `feishu_chat` for chat management operations.

## Actions

### Get Chat Info

```json
{ "action": "get", "chat_id": "oc_xxx" }
```

Returns: chat name, description, owner, member count, chat type (group/p2p).

### List Chat Members

```json
{ "action": "members", "chat_id": "oc_xxx" }
```

Returns: all members in the chat with their user info.

### Create Group Chat

```json
{
  "action": "create",
  "name": "Project Team",
  "description": "Project discussion group",
  "member_ids": ["ou_xxx", "ou_yyy"]
}
```

### Add Members

```json
{
  "action": "add_members",
  "chat_id": "oc_xxx",
  "member_ids": ["ou_zzz"]
}
```

### Remove Members

```json
{
  "action": "remove_members",
  "chat_id": "oc_xxx",
  "member_ids": ["ou_zzz"]
}
```

Requires admin permissions.

## Chat Types

| Type    | Description                          |
| ------- | ------------------------------------ |
| `p2p`   | Private chat between two users       |
| `group` | Group chat with multiple members     |

## Notes

- Bots can only manage chats they are members of
- Some actions (remove members, update settings) require admin permissions
- Member IDs use `open_id` format (`ou_xxx`)

## Permissions

Required: `im:chat`, `im:chat:readonly`
