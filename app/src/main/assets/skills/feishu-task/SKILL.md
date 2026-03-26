---
name: feishu-task
description: |
  Feishu task management. Activate when user mentions tasks, todos, or task lists.
---

# Feishu Task Tool

Tool `feishu_task` for task management operations.

## Actions

### List Tasks

```json
{ "action": "list", "page_size": 20 }
```

Returns: user's tasks with details.

### Get Task Details

```json
{ "action": "get", "task_id": "task_xxx" }
```

Returns: complete task information including title, description, status, assignee, due date.

### Create Task

```json
{
  "action": "create",
  "title": "New Task",
  "description": "Task description",
  "due_date": "2024-12-31",
  "assignee_id": "ou_xxx"
}
```

### Update Task

```json
{
  "action": "update",
  "task_id": "task_xxx",
  "title": "Updated Title",
  "status": "completed"
}
```

## Task Status

| Status        | Description |
| ------------- | ----------- |
| `todo`        | Not started |
| `in_progress` | In progress |
| `completed`   | Completed   |
| `canceled`    | Canceled    |

## Permissions

Required: `task:task`, `task:task:readonly`
