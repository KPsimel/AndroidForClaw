---
name: feishu-doc
description: |
  Feishu document read/write operations. Activate when user mentions Feishu docs, cloud docs, or docx links.
---

# Feishu Document Tool

Single tool `feishu_doc` with action parameter for all document operations.

## Token Extraction

From URL `https://xxx.feishu.cn/docx/ABC123def` → `document_id` = `ABC123def`

## Actions

### Read Document

```json
{ "action": "read", "document_id": "ABC123def" }
```

Returns: title, plain text content, block statistics. Check `hint` field - if present, structured content (tables, images) exists that requires `list_blocks`.

### Write Document (Replace All)

```json
{ "action": "write", "document_id": "ABC123def", "content": "# Title\n\nMarkdown content..." }
```

Replaces entire document with markdown content. Supports: headings, lists, code blocks, quotes, links, bold/italic/strikethrough.

**Limitation:** Markdown tables are NOT supported in write mode. Use `create_table` for tables.

### Append Content

```json
{ "action": "append", "document_id": "ABC123def", "content": "Additional content" }
```

Appends markdown to end of document.

### Create Document

```json
{ "action": "create", "title": "New Document", "folder_id": "fldcnXXX" }
```

Creates a new document. Optional `folder_id` to place in specific folder.

### List Blocks

```json
{ "action": "list_blocks", "document_id": "ABC123def" }
```

Returns full block data including tables, images. Use this to read structured content.

### Get Single Block

```json
{ "action": "get_block", "document_id": "ABC123def", "block_id": "doxcnXXX" }
```

### Update Block Text

```json
{ "action": "update_block", "document_id": "ABC123def", "block_id": "doxcnXXX", "content": "New text" }
```

### Delete Block

```json
{ "action": "delete_block", "document_id": "ABC123def", "block_id": "doxcnXXX" }
```

## Reading Workflow

1. Start with `action: "read"` - get plain text + statistics
2. Check `block_types` in response for Table, Image, Code, etc.
3. If structured content exists, use `action: "list_blocks"` for full data

## Permissions

Required: `docx:document`, `docx:document:readonly`, `drive:drive`

**Note:** `feishu_wiki` depends on this tool - wiki page content is read/written via `feishu_doc`.
