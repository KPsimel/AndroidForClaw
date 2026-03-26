/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/tools/sessions-spawn-tool.ts
 *
 * AndroidForClaw adaptation: LLM-facing tool to spawn subagent sessions.
 */
package com.xiaomo.androidforclaw.agent.tools

import com.xiaomo.androidforclaw.agent.loop.AgentLoop
import com.xiaomo.androidforclaw.agent.subagent.SPAWN_ACCEPTED_NOTE
import com.xiaomo.androidforclaw.agent.subagent.SpawnMode
import com.xiaomo.androidforclaw.agent.subagent.SpawnStatus
import com.xiaomo.androidforclaw.agent.subagent.SpawnSubagentParams
import com.xiaomo.androidforclaw.agent.subagent.SubagentSpawner
import com.xiaomo.androidforclaw.logging.Log
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * sessions_spawn — Spawn an isolated subagent to handle a task.
 * Aligned with OpenClaw createSessionsSpawnTool.
 */
class SessionsSpawnTool(
    private val spawner: SubagentSpawner,
    private val parentSessionKey: String,
    private val parentAgentLoop: AgentLoop,
    private val parentDepth: Int,
) : Tool {
    companion object {
        private const val TAG = "SessionsSpawnTool"
    }

    override val name = "sessions_spawn"
    override val description = "Spawn an isolated subagent session to handle a specific task in parallel. " +
        "The subagent runs independently with its own context and tools, and automatically reports " +
        "results back when complete. Use this to parallelize independent tasks."

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "task" to PropertySchema(
                            type = "string",
                            description = "The task description for the subagent to execute."
                        ),
                        "label" to PropertySchema(
                            type = "string",
                            description = "Short display label for this subagent (e.g. 'research-api', 'analyze-logs')."
                        ),
                        "model" to PropertySchema(
                            type = "string",
                            description = "Model override for this subagent (format: 'provider/model-id'). Defaults to parent model."
                        ),
                        "timeout_seconds" to PropertySchema(
                            type = "number",
                            description = "Run timeout in seconds. Default: 300."
                        ),
                        "mode" to PropertySchema(
                            type = "string",
                            description = "Spawn mode: 'run' (one-shot, default) or 'session' (persistent).",
                            enum = listOf("run", "session")
                        ),
                        "thinking" to PropertySchema(
                            type = "string",
                            description = "Thinking/reasoning level: 'none', 'brief', 'verbose'. Default: inherit from parent."
                        ),
                    ),
                    required = listOf("task")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val task = args["task"] as? String
        if (task.isNullOrBlank()) {
            return ToolResult.error("Missing required parameter: task")
        }

        val label = (args["label"] as? String)?.trim()?.ifBlank { null }
            ?: task.take(40).replace('\n', ' ')
        val model = args["model"] as? String
        val timeoutSeconds = (args["timeout_seconds"] as? Number)?.toInt()
        val modeStr = args["mode"] as? String
        val mode = when (modeStr?.lowercase()) {
            "session" -> SpawnMode.SESSION
            else -> SpawnMode.RUN
        }

        Log.i(TAG, "Spawning subagent: label=$label, model=$model, timeout=$timeoutSeconds, mode=$mode")

        val thinking = args["thinking"] as? String

        val params = SpawnSubagentParams(
            task = task,
            label = label,
            model = model,
            thinking = thinking,
            runTimeoutSeconds = timeoutSeconds,
            mode = mode,
        )

        val result = spawner.spawn(params, parentSessionKey, parentAgentLoop, parentDepth)

        return when (result.status) {
            SpawnStatus.ACCEPTED -> ToolResult(
                success = true,
                content = buildString {
                    appendLine("Subagent spawned successfully.")
                    appendLine("Run ID: ${result.runId}")
                    appendLine("Session: ${result.childSessionKey}")
                    appendLine("Mode: ${result.mode?.wireValue ?: "run"}")
                    if (result.modelApplied != null) {
                        appendLine("Model: ${result.modelApplied}")
                    }
                    appendLine()
                    appendLine(SPAWN_ACCEPTED_NOTE)
                },
                metadata = mapOf(
                    "status" to "accepted",
                    "run_id" to (result.runId ?: ""),
                    "child_session_key" to (result.childSessionKey ?: ""),
                )
            )

            SpawnStatus.FORBIDDEN -> ToolResult(
                success = false,
                content = "Spawn forbidden: ${result.note ?: result.error ?: "unknown reason"}",
                metadata = mapOf("status" to "forbidden")
            )

            SpawnStatus.ERROR -> ToolResult(
                success = false,
                content = "Spawn error: ${result.error ?: "unknown error"}",
                metadata = mapOf("status" to "error")
            )
        }
    }
}
