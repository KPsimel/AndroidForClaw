/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/tools/sessions-list-tool.ts
 * - ../openclaw/src/agents/subagent-control.ts (buildSubagentList)
 *
 * AndroidForClaw adaptation: LLM-facing tool to list subagent runs.
 * Format aligned with OpenClaw buildSubagentList.
 */
package com.xiaomo.androidforclaw.agent.tools

import com.xiaomo.androidforclaw.agent.subagent.SubagentRegistry
import com.xiaomo.androidforclaw.agent.subagent.SubagentRunStatus
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * sessions_list — List active and recent subagent runs.
 * Aligned with OpenClaw buildSubagentList ordering and formatting.
 */
class SessionsListTool(
    private val registry: SubagentRegistry,
    private val parentSessionKey: String,
) : Tool {

    override val name = "sessions_list"
    override val description = "List active and recent subagent runs spawned by this session."

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "status" to PropertySchema(
                            type = "string",
                            description = "Filter: 'active' (running only) or 'all' (including completed). Default: 'all'.",
                            enum = listOf("active", "all")
                        )
                    ),
                    required = emptyList()
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val statusFilter = (args["status"] as? String)?.lowercase() ?: "all"

        // Use indexed list (active first, then recent) — aligned with OpenClaw
        val indexed = registry.buildIndexedList(parentSessionKey)
        val runs = when (statusFilter) {
            "active" -> indexed.filter { it.isActive }
            else -> indexed
        }

        if (runs.isEmpty()) {
            return ToolResult(
                success = true,
                content = "No subagent runs found (filter: $statusFilter).",
            )
        }

        val text = buildString {
            // Separate active and recent sections (aligned with OpenClaw buildSubagentList)
            val active = runs.filter { it.isActive }
            val recent = runs.filter { !it.isActive }

            if (active.isNotEmpty() || statusFilter == "all") {
                appendLine("Active subagents:")
                if (active.isEmpty()) {
                    appendLine("  (none)")
                } else {
                    for ((i, run) in active.withIndex()) {
                        val pendingChildren = registry.countPendingDescendantRuns(run.childSessionKey)
                        val status = if (pendingChildren > 0) {
                            "active (waiting on $pendingChildren children)"
                        } else {
                            "active"
                        }
                        val runtime = formatDurationCompact(run.runtimeMs)
                        val model = run.model?.let { " ($it)" } ?: ""
                        val taskSnippet = if (run.task != run.label) ", ${run.task.take(80)}" else ""
                        appendLine("  ${i + 1}. ${run.label}$model, $runtime [$status]$taskSnippet")
                    }
                }
                appendLine()
            }

            if (recent.isNotEmpty() && statusFilter != "active") {
                appendLine("Recent (completed):")
                val offset = active.size
                for ((i, run) in recent.withIndex()) {
                    val status = when (run.outcome?.status) {
                        SubagentRunStatus.OK -> "done"
                        SubagentRunStatus.TIMEOUT -> "timeout"
                        SubagentRunStatus.ERROR -> "failed"
                        else -> run.outcome?.status?.wireValue ?: "unknown"
                    }
                    val runtime = formatDurationCompact(run.runtimeMs)
                    val model = run.model?.let { " ($it)" } ?: ""
                    val error = run.outcome?.error?.let { " - $it" } ?: ""
                    appendLine("  ${offset + i + 1}. ${run.label}$model, $runtime [$status]$error")
                }
            }
        }.trimEnd()

        return ToolResult(success = true, content = text)
    }

    companion object {
        /** Format duration compactly (aligned with OpenClaw formatDurationCompact) */
        fun formatDurationCompact(ms: Long): String {
            return when {
                ms < 1000 -> "${ms}ms"
                ms < 60_000 -> "${ms / 1000}s"
                ms < 3600_000 -> "${ms / 60_000}m${(ms % 60_000) / 1000}s"
                else -> "${ms / 3600_000}h${(ms % 3600_000) / 60_000}m"
            }
        }
    }
}
