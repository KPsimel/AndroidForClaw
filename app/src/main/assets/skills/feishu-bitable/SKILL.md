---
name: feishu-bitable
description: |
  Feishu multi-dimensional table (Bitable) operations. Activate when user mentions tables, databases, or bitable links.
---

# Feishu Bitable Tool

Tool `feishu_bitable` for multi-dimensional table operations.

## Token Extraction

From URL `https://xxx.feishu.cn/base/ABC123def` → `app_token` = `ABC123def`

## Actions

### List Tables

```json
{ "action": "list_tables", "app_token": "ABC123def" }
```

Returns: all tables in the bitable app.

### Get Table Info

```json
{ "action": "get_table", "app_token": "ABC123def", "table_id": "tblXXX" }
```

Returns: table name, fields, record count.

### Query Records

```json
{
  "action": "query_records",
  "app_token": "ABC123def",
  "table_id": "tblXXX",
  "filter": "field_name=\"value\"",
  "page_size": 20
}
```

Returns: records matching filter criteria.

### Create Record

```json
{
  "action": "create_record",
  "app_token": "ABC123def",
  "table_id": "tblXXX",
  "fields": { "field1": "value1", "field2": "value2" }
}
```

### Update Record

```json
{
  "action": "update_record",
  "app_token": "ABC123def",
  "table_id": "tblXXX",
  "record_id": "recXXX",
  "fields": { "field1": "new_value" }
}
```

### Delete Record

```json
{
  "action": "delete_record",
  "app_token": "ABC123def",
  "table_id": "tblXXX",
  "record_id": "recXXX"
}
```

## Field Types

- **Text**: Single line text, multi-line text
- **Number**: Integer, decimal
- **Select**: Single select, multi-select
- **Date**: Date, datetime
- **Checkbox**: Boolean
- **Attachment**: Files
- **User**: User picker
- **Formula**: Calculated fields

## Permissions

Required: `bitable:app`, `bitable:app:readonly`
