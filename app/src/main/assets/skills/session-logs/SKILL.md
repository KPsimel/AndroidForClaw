---
name: session-logs
description: Search and analyze session logs (conversation history) using text search and filtering. Use when user asks about prior conversations, what was discussed before, or historical context.
metadata: { "openclaw": { "emoji": "📜", "always": false } }
---

# Session Logs

Search your complete conversation history stored in session files. Use this when a user references older conversations or asks what was said before.

## 🎯 When to Use

Use this skill when the user asks about:
- "What did we discuss last time?"
- "What tasks did you complete yesterday?"
- "Show me our conversation history"
- "What did I ask you about earlier?"
- "Find when we talked about [topic]"

## 📁 Session Storage

Session logs are stored at:
- **Android**: `/sdcard/.androidforclaw/sessions/`
- **Format**: JSON files per session

Each session file contains:
- Timestamp
- User messages
- Assistant responses
- Tool calls and results
- System events

## 🔍 Available Operations

### 1. List Recent Sessions

```kotlin
// Use list_dir tool to see session files
list_dir(path: "/sdcard/.androidforclaw/sessions")
```

Returns list of session files with timestamps.

### 2. Search Session Content

```kotlin
// Use read_file tool to read specific session
read_file(path: "/sdcard/.androidforclaw/sessions/session_20260308_143022.json")
```

### 3. Filter by Keywords

When searching session content:
1. Read session files using `read_file`
2. Parse JSON content
3. Search for keywords in messages
4. Extract relevant context

## 📝 Search Strategy

### Step 1: List Available Sessions

```kotlin
// Get all session files
list_dir(path: "/sdcard/.androidforclaw/sessions")
```

### Step 2: Read Recent Sessions

```kotlin
// Read the most recent sessions first
read_file(path: "/sdcard/.androidforclaw/sessions/session_YYYYMMDD_HHMMSS.json")
```

### Step 3: Search for Keywords

Look for:
- User queries containing keywords
- Assistant responses about topic
- Tool calls related to task
- Timestamps of relevant conversations

### Step 4: Present Results

Format results showing:
- **When**: Date and time of conversation
- **Topic**: What was discussed
- **Key Actions**: What tools were used
- **Summary**: Brief summary of that conversation

## 💡 Example Workflow

**User asks**: "What did we discuss about mobile testing yesterday?"

**Your response**:
1. List sessions from yesterday
2. Read each session file
3. Search for "mobile" and "testing" keywords
4. Extract relevant conversations
5. Summarize findings with timestamps

```
Found 2 conversations about mobile testing yesterday:

1. **2026-03-07 14:30** - Mobile Testing Setup
   - Discussed test automation frameworks
   - Set up Espresso test environment
   - Ran 5 UI tests successfully

2. **2026-03-07 16:45** - Bug Investigation
   - Debugged login flow issue
   - Used screenshot tool to capture error
   - Fixed null pointer exception
```

## 🔧 Tools to Use

1. **list_dir** - List session files
2. **read_file** - Read session content
3. **javascript_exec** - Parse JSON and filter data

Example JavaScript for filtering:

```javascript
// Parse session file and search
const session = JSON.parse(sessionContent);
const results = session.messages.filter(msg => {
  const text = msg.content.toLowerCase();
  return text.includes(keyword1) || text.includes(keyword2);
});
return results;
```

## 📊 Session File Structure

```json
{
  "sessionId": "session_20260308_143022",
  "startTime": "2026-03-08T14:30:22Z",
  "messages": [
    {
      "role": "user",
      "content": "Test the login flow",
      "timestamp": "2026-03-08T14:30:22Z"
    },
    {
      "role": "assistant",
      "content": "I'll test the login flow...",
      "timestamp": "2026-03-08T14:30:25Z"
    },
    {
      "role": "tool",
      "name": "screenshot",
      "result": "...",
      "timestamp": "2026-03-08T14:30:30Z"
    }
  ],
  "summary": "Tested login flow, found 2 issues",
  "endTime": "2026-03-08T14:45:00Z"
}
```

## ⚠️ Important Notes

### Session Privacy

- Sessions contain full conversation history
- May include sensitive information (credentials, personal data)
- Only search when user explicitly asks
- Don't proactively reveal old conversations

### Performance

- Session files can be large (1-10 MB each)
- Read most recent sessions first
- Use keyword filtering to reduce data
- Limit results to top 5-10 most relevant

### Limitations

- Sessions only stored locally on device
- No full-text search index (yet)
- Must read files sequentially
- Large history may take time to search

## 🎓 Best Practices

1. **Ask for timeframe**: "When did this conversation happen?"
2. **Use specific keywords**: More specific = better results
3. **Limit scope**: "Last week" vs "all time"
4. **Summarize findings**: Don't dump raw session data
5. **Respect privacy**: Only reveal what user asks for

## 🔮 Future Improvements

Currently planned:
- Full-text search indexing
- Session summarization
- Conversation threading
- Export to markdown
- Search by tool calls
- Search by date range

---

**Remember**: Session logs are a powerful memory system. Use them to provide context-aware assistance based on past conversations.
