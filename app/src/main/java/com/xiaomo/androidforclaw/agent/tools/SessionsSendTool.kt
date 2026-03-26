/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/tools/sessions-send-tool.ts
 * - ../openclaw/src/agents/subagent-control.ts (steerControlledSubagentRun, sendControlledSubagentMessage)
 *
 * AndroidForClaw adaptation: LLM-facing tool to send messages to running subagents.
 * Supports multi-strategy target resolution and fire-and-forget / wait modes.
 * Steer semantics: abort current run + restart with new message (aligned with OpenClaw).
 */
package com.xiaomo.androidforclaw.agent.tools

import com.xiaomo.androidforclaw.agent.loop.AgentLoop
import com.xiaomo.androidforclaw.agent.subagent.SubagentSpawner
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * sessions_send — Send a message to a running subagent (steer = abort + restart).
 * Aligned with OpenClaw steerControlledSubagentRun + sendControlledSubagentMessage.
 *
 * Target resolution supports: "last", numeric index, session key, label, label prefix, runId prefix.
 */
class SessionsSendTool(
    private val spawner: SubagentSpawner,
    private val parentSessionKey: String,
    private val parentAgentLoop: AgentLoop,
) : Tool {

    override val name = "sessions_send"
    override val description = "Send a message to a running subagent to steer or redirect its work. " +
        "This aborts the subagent's current run and restarts it with the new message. " +
        "Target can be 'last', a numeric index, a label, or a run ID."

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
                        "message" to PropertySchema(
                            type = "string",
                            description = "The message to send to the subagent."
                        ),
                        "timeout_seconds" to PropertySchema(
                            type = "number",
                            description = "Wait timeout in seconds. 0 = fire-and-forget (default). >0 = wait for completion."
                        ),
                    ),
                    required = listOf("target", "message")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val target = args["target"] as? String
        if (target.isNullOrBlank()) {
            return ToolResult.error("Missing required parameter: target")
        }
        val message = args["message"] as? String
        if (message.isNullOrBlank()) {
            return ToolResult.error("Missing required parameter: message")
        }
        val timeoutSeconds = (args["timeout_seconds"] as? Number)?.toInt() ?: 0

        // Resolve target using multi-strategy resolution
        val record = spawner.registry.resolveTarget(target, parentSessionKey)
            ?: return ToolResult(success = false, content = "No matching subagent found for target: $target")

        // Steer (abort + restart, aligned with OpenClaw)
        val (success, info) = spawner.steer(
            runId = record.runId,
            message = message,
            callerSessionKey = parentSessionKey,
            parentAgentLoop = parentAgentLoop,
        )

        if (!success) {
            return ToolResult(success = false, content = "Steer failed: $info")
        }

        // Fire-and-forget mode
        if (timeoutSeconds <= 0) {
            return ToolResult(success = true, content = "Message sent to ${record.label}: $info")
        }

        // Wait mode: poll for child completion
        val waitMs = timeoutSeconds * 1000L
        val startWait = System.currentTimeMillis()
        while (System.currentTimeMillis() - startWait < waitMs) {
            val latestRecord = spawner.registry.getRunByChildSessionKey(record.childSessionKey)
            if (latestRecord != null && !latestRecord.isActive) {
                return ToolResult(
                    success = true,
                    content = buildString {
                        appendLine("Subagent '${latestRecord.label}' completed after steer.")
                        appendLine("Status: ${latestRecord.outcome?.status?.wireValue ?: "unknown"}")
                        latestRecord.frozenResultText?.let { text ->
                            appendLine("Result: ${text.take(4000)}")
                        }
                    }
                )
            }
            kotlinx.coroutines.delay(500)
        }

        return ToolResult(
            success = true,
            content = "Steer sent to ${record.label}. Child still running after ${timeoutSeconds}s wait."
        )
    }
}
