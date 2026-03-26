/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/tools/sessions-history-tool.ts
 *
 * AndroidForClaw adaptation: LLM-facing tool to read subagent conversation history.
 * Reads from AgentLoop.conversationMessages for active runs, or frozenResultText for completed runs.
 * Includes per-field truncation (4000 chars) and total cap (80KB), aligned with OpenClaw.
 */
package com.xiaomo.androidforclaw.agent.tools

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

        // Get messages from AgentLoop (active) or frozenResultText (completed)
        val loop = registry.getAgentLoop(record.runId)
        val messages = loop?.conversationMessages

        if (messages.isNullOrEmpty()) {
            // Fallback to frozen result for completed runs
            val frozen = record.frozenResultText
            if (!frozen.isNullOrBlank()) {
                val truncated = if (frozen.length > TEXT_MAX_CHARS) {
                    frozen.take(TEXT_MAX_CHARS) + "...(truncated)"
                } else frozen
                return ToolResult(
                    success = true,
                    content = buildString {
                        appendLine("Session: ${record.childSessionKey} (completed)")
                        appendLine("Status: ${record.outcome?.status?.wireValue ?: "unknown"}")
                        appendLine()
                        appendLine("[assistant] $truncated")
                    },
                    metadata = mapOf("sessionKey" to record.childSessionKey, "messages" to 1)
                )
            }
            return ToolResult(success = true, content = "No message history available for '${record.label}'.")
        }

        // Filter and format messages
        val filtered = messages.takeLast(limit).filter { msg ->
            includeTools || (msg.role != "tool" && msg.toolCalls.isNullOrEmpty())
        }

        var totalBytes = 0
        val formatted = buildString {
            appendLine("Session: ${record.childSessionKey} (${if (record.isActive) "active" else "completed"})")
            appendLine("Messages: ${filtered.size}/${messages.size}")
            appendLine()

            for (msg in filtered) {
                val content = msg.content.let { text ->
                    if (text.length > TEXT_MAX_CHARS) {
                        text.take(TEXT_MAX_CHARS) + "...(truncated)"
                    } else text
                }
                val line = "[${msg.role}] $content\n\n"

                if (totalBytes + line.toByteArray().size > MAX_BYTES) {
                    appendLine("...(truncated, exceeded ${MAX_BYTES / 1024}KB limit)")
                    break
                }
                append(line)
                totalBytes += line.toByteArray().size
            }
        }.trimEnd()

        return ToolResult(
            success = true,
            content = formatted,
            metadata = mapOf(
                "sessionKey" to record.childSessionKey,
                "messages" to filtered.size,
                "bytes" to totalBytes,
            )
        )
    }
}
