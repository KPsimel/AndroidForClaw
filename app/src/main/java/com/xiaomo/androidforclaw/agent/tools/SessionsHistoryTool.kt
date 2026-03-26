/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/tools/sessions-history-tool.ts
 * - ../openclaw/src/logging/redact.ts (redactSensitiveText)
 *
 * AndroidForClaw adaptation: LLM-facing tool to read subagent conversation history.
 * Reads from AgentLoop.conversationMessages for active runs, or frozenResultText for completed runs.
 * Includes per-field truncation (4000 chars), sensitive text redaction, and total cap (80KB).
 * Aligned with OpenClaw sessions-history-tool + redact pipeline.
 */
package com.xiaomo.androidforclaw.agent.tools

import com.xiaomo.androidforclaw.agent.subagent.SessionAccessResult
import com.xiaomo.androidforclaw.agent.subagent.SessionVisibilityGuard
import com.xiaomo.androidforclaw.agent.subagent.SubagentRegistry
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * sessions_history — Read conversation history of a child subagent session.
 * Aligned with OpenClaw createSessionsHistoryTool.
 */
class SessionsHistoryTool(
    private val registry: SubagentRegistry,
    private val parentSessionKey: String,
) : Tool {

    companion object {
        /** Maximum chars per field (aligned with OpenClaw SESSIONS_HISTORY_TEXT_MAX_CHARS) */
        private const val TEXT_MAX_CHARS = 4000
        /** Maximum total output bytes (aligned with OpenClaw SESSIONS_HISTORY_MAX_BYTES) */
        private const val MAX_BYTES = 80 * 1024  // 80KB
        /** Chunk size for bounded regex replacement to avoid catastrophic backtracking */
        private const val REGEX_CHUNK_SIZE = 16_384

        /**
         * Sensitive text redaction patterns.
         * Aligned with OpenClaw DEFAULT_REDACT_PATTERNS from logging/redact.ts.
         */
        private val REDACT_PATTERNS: List<Regex> by lazy {
            listOf(
                // ENV-style: KEY=value, TOKEN=value, SECRET=value, PASSWORD=value, PASSWD=value
                Regex("""(?i)((?:API[_-]?KEY|TOKEN|SECRET|PASSWORD|PASSWD)\s*[=:]\s*)(\S+)"""),
                // JSON fields: "apiKey": "...", "token": "...", etc.
                Regex("""(?i)("(?:api[_-]?key|token|secret|password|passwd|access[_-]?token|refresh[_-]?token)"\s*:\s*")([^"]+)"""),
                // CLI flags: --api-key value, --token value, etc.
                Regex("""(?i)(--(?:api-key|token|secret|password|passwd)\s+)(\S+)"""),
                // Authorization: Bearer ...
                Regex("""(Authorization:\s*Bearer\s+)(\S{18,})"""),
                // Bare Bearer token
                Regex("""(Bearer\s+)(\S{18,})"""),
                // PEM private keys
                Regex("""-----BEGIN\s+[A-Z\s]*PRIVATE KEY-----[\s\S]*?-----END\s+[A-Z\s]*PRIVATE KEY-----"""),
                // OpenAI-style: sk-...
                Regex("""(sk-[A-Za-z0-9]{8,})"""),
                // GitHub PATs: ghp_... and github_pat_...
                Regex("""(ghp_[A-Za-z0-9]{20,})"""),
                Regex("""(github_pat_[A-Za-z0-9]{20,})"""),
                // Slack tokens: xox[baprs]-... and xapp-...
                Regex("""(xox[baprs]-[A-Za-z0-9\-]{10,})"""),
                Regex("""(xapp-[A-Za-z0-9\-]{10,})"""),
                // Groq keys: gsk_...
                Regex("""(gsk_[A-Za-z0-9]{10,})"""),
                // Google AI keys: AIza...
                Regex("""(AIza[A-Za-z0-9_\-]{20,})"""),
                // Perplexity keys: pplx-...
                Regex("""(pplx-[A-Za-z0-9]{10,})"""),
                // npm tokens: npm_...
                Regex("""(npm_[A-Za-z0-9]{10,})"""),
                // Telegram bot tokens: 123456789:ABC-DEF...
                Regex("""(\d{8,10}:[A-Za-z0-9_\-]{30,})"""),
            )
        }

        /**
         * Mask a token value. Aligned with OpenClaw maskToken.
         * Short tokens (<18 chars) → "***"
         * Long tokens → first 6 + "..." + last 4
         */
        private fun maskToken(token: String): String {
            return if (token.length < 18) {
                "***"
            } else {
                "${token.take(6)}...${token.takeLast(4)}"
            }
        }

        /**
         * Redact sensitive text patterns.
         * Aligned with OpenClaw redactSensitiveText.
         * Uses bounded replacement for large texts to avoid regex performance issues.
         */
        fun redactSensitiveText(text: String): Pair<String, Boolean> {
            if (text.isEmpty()) return Pair(text, false)

            var result = text
            var redacted = false

            for (pattern in REDACT_PATTERNS) {
                val newResult = if (result.length > REGEX_CHUNK_SIZE * 2) {
                    replacePatternBounded(result, pattern)
                } else {
                    pattern.replace(result) { match ->
                        redacted = true
                        when {
                            // PEM key block
                            match.value.startsWith("-----BEGIN") ->
                                "-----BEGIN PRIVATE KEY-----\n...redacted...\n-----END PRIVATE KEY-----"
                            // Patterns with prefix group + token group
                            match.groupValues.size >= 3 && match.groupValues[1].isNotEmpty() ->
                                "${match.groupValues[1]}${maskToken(match.groupValues[2])}"
                            // Standalone token patterns (sk-, ghp_, etc.)
                            else -> maskToken(match.value)
                        }
                    }
                }
                if (newResult != result) {
                    redacted = true
                    result = newResult
                }
            }

            return Pair(result, redacted)
        }

        /**
         * Bounded regex replacement for large texts.
         * Aligned with OpenClaw replacePatternBounded.
         * Processes in chunks to avoid catastrophic backtracking.
         */
        private fun replacePatternBounded(text: String, pattern: Regex): String {
            val sb = StringBuilder()
            var offset = 0
            while (offset < text.length) {
                val end = minOf(offset + REGEX_CHUNK_SIZE, text.length)
                val chunk = text.substring(offset, end)
                sb.append(pattern.replace(chunk) { match ->
                    when {
                        match.value.startsWith("-----BEGIN") ->
                            "-----BEGIN PRIVATE KEY-----\n...redacted...\n-----END PRIVATE KEY-----"
                        match.groupValues.size >= 3 && match.groupValues[1].isNotEmpty() ->
                            "${match.groupValues[1]}${maskToken(match.groupValues[2])}"
                        else -> maskToken(match.value)
                    }
                })
                offset = end
            }
            return sb.toString()
        }

        /**
         * Truncate and redact history text.
         * Aligned with OpenClaw truncateHistoryText.
         * Returns (sanitized text, truncated, redacted).
         */
        fun truncateHistoryText(text: String): Triple<String, Boolean, Boolean> {
            val (redactedText, wasRedacted) = redactSensitiveText(text)
            return if (redactedText.length > TEXT_MAX_CHARS) {
                Triple(redactedText.take(TEXT_MAX_CHARS) + "\n...(truncated)...", true, wasRedacted)
            } else {
                Triple(redactedText, false, wasRedacted)
            }
        }
    }

    override val name = "sessions_history"
    override val description = "Read the conversation history of a child subagent session."

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "target" to PropertySchema(
                            type = "string",
                            description = "Target subagent: 'last', numeric index (1-based), label, label prefix, run ID, or session key."
                        ),
                        "limit" to PropertySchema(
                            type = "number",
                            description = "Maximum number of messages to return. Default: 50."
                        ),
                        "include_tools" to PropertySchema(
                            type = "boolean",
                            description = "Include tool call/result messages. Default: false."
                        ),
                    ),
                    required = listOf("target")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val target = args["target"] as? String
        if (target.isNullOrBlank()) {
            return ToolResult.error("Missing required parameter: target")
        }
        val limit = (args["limit"] as? Number)?.toInt() ?: 50
        val includeTools = (args["include_tools"] as? Boolean) ?: false

        // Resolve target
        val record = registry.resolveTarget(target, parentSessionKey)
            ?: return ToolResult(success = false, content = "No matching subagent found for target: $target")

        // Visibility guard (aligned with OpenClaw controlScope)
        val visibility = SessionVisibilityGuard.resolveVisibility(parentSessionKey, registry)
        val access = SessionVisibilityGuard.checkAccess(
            "read history of", parentSessionKey, record.childSessionKey, visibility, registry
        )
        if (access is SessionAccessResult.Denied) {
            return ToolResult(success = false, content = access.reason)
        }

        // Get messages from AgentLoop (active) or frozenResultText (completed)
        val loop = registry.getAgentLoop(record.runId)
        val messages = loop?.conversationMessages

        if (messages.isNullOrEmpty()) {
            // Fallback to frozen result for completed runs
            val frozen = record.frozenResultText
            if (!frozen.isNullOrBlank()) {
                val (sanitized, truncated, redacted) = truncateHistoryText(frozen)
                return ToolResult(
                    success = true,
                    content = buildString {
                        appendLine("Session: ${record.childSessionKey} (completed)")
                        appendLine("Status: ${record.outcome?.status?.wireValue ?: "unknown"}")
                        appendLine()
                        appendLine("[assistant] $sanitized")
                    },
                    metadata = mapOf(
                        "sessionKey" to record.childSessionKey,
                        "messages" to 1,
                        "truncated" to truncated,
                        "contentRedacted" to redacted,
                    )
                )
            }
            return ToolResult(success = true, content = "No message history available for '${record.label}'.")
        }

        // Filter and format messages with sanitization
        val filtered = messages.takeLast(limit).filter { msg ->
            includeTools || (msg.role != "tool" && msg.toolCalls.isNullOrEmpty())
        }

        var totalBytes = 0
        var anyTruncated = false
        var anyRedacted = false
        var droppedMessages = false

        // Build lines newest→oldest to cap correctly (drop oldest first, aligned with OpenClaw)
        val lines = mutableListOf<String>()
        for (msg in filtered.reversed()) {
            val (sanitizedContent, truncated, redacted) = truncateHistoryText(msg.content)
            if (truncated) anyTruncated = true
            if (redacted) anyRedacted = true

            val line = "[${msg.role}] $sanitizedContent\n\n"
            val lineBytes = line.toByteArray(Charsets.UTF_8).size

            if (totalBytes + lineBytes > MAX_BYTES) {
                droppedMessages = true
                break
            }
            lines.add(line)
            totalBytes += lineBytes
        }
        lines.reverse() // Back to chronological order

        val formatted = buildString {
            appendLine("Session: ${record.childSessionKey} (${if (record.isActive) "active" else "completed"})")
            appendLine("Messages: ${lines.size}/${messages.size}")
            if (droppedMessages) appendLine("(oldest messages dropped, exceeded ${MAX_BYTES / 1024}KB limit)")
            appendLine()

            for (line in lines) {
                append(line)
            }
        }.trimEnd()

        // Hard cap safety net: if even after truncation we exceed MAX_BYTES
        val finalFormatted = if (formatted.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
            droppedMessages = true
            "[sessions_history omitted: output too large]"
        } else {
            formatted
        }

        return ToolResult(
            success = true,
            content = finalFormatted,
            metadata = mapOf(
                "sessionKey" to record.childSessionKey,
                "messages" to filtered.size,
                "bytes" to totalBytes,
                "truncated" to (anyTruncated || droppedMessages),
                "droppedMessages" to droppedMessages,
                "contentRedacted" to anyRedacted,
            )
        )
    }
}
