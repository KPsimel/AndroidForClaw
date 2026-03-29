package com.xiaomo.androidforclaw.agent.context

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/tool-policy-pipeline.ts (buildDefaultToolPolicyPipelineSteps, applyToolPolicyPipeline)
 * - ../openclaw/src/agents/tool-policy.ts (isOwnerOnlyToolName, applyOwnerOnlyToolPolicy, ToolPolicyLike, ToolProfileId)
 *
 * AndroidForClaw adaptation: multi-step tool policy pipeline.
 * Filters tools through ordered policy steps: profile, global, agent, group, owner-only, subagent.
 */

import com.xiaomo.androidforclaw.logging.Log

/**
 * Tool policy definition (allow/deny lists).
 * Aligned with OpenClaw ToolPolicyLike.
 */
data class ToolPolicyLike(
    val allow: List<String>? = null,
    val deny: List<String>? = null
)

/**
 * Tool profile IDs for preset tool sets.
 * Aligned with OpenClaw ToolProfileId.
 */
enum class ToolProfileId(val id: String) {
    MINIMAL("minimal"),
    CODING("coding"),
    MESSAGING("messaging"),
    FULL("full");

    companion object {
        fun fromString(s: String?): ToolProfileId? =
            entries.find { it.id == s?.lowercase() }
    }
}

/**
 * A single step in the tool policy pipeline.
 * Aligned with OpenClaw ToolPolicyPipelineStep.
 */
data class ToolPolicyPipelineStep(
    val policy: ToolPolicyLike?,
    val label: String
)

/**
 * Built-in tool groups for policy expansion.
 * Aligned with OpenClaw TOOL_GROUPS.
 */
object ToolGroups {
    val GROUPS: Map<String, List<String>> = mapOf(
        "files" to listOf("read_file", "write_file", "edit_file", "list_dir"),
        "runtime" to listOf("exec"),
        "web" to listOf("web_search", "web_fetch"),
        "memory" to listOf("memory_search", "memory_get"),
        "sessions" to listOf("sessions_list", "sessions_history", "sessions_send", "sessions_spawn", "sessions_yield", "sessions_kill", "session_status", "subagents"),
        "ui" to listOf("canvas", "browser"),
        "media" to listOf("tts", "eye", "feishu_send_image"),
        "config" to listOf("config_get", "config_set"),
        "automation" to listOf("cron")
    )

    /** Expand group references in tool names list */
    fun expandToolGroups(names: List<String>?): List<String>? {
        if (names == null) return null
        val expanded = mutableListOf<String>()
        for (name in names) {
            val group = GROUPS[name.removePrefix("group:")]
            if (group != null) {
                expanded.addAll(group)
            } else {
                expanded.add(name)
            }
        }
        return expanded
    }
}

/**
 * Owner-only tools that require sender to be the device owner.
 * Aligned with OpenClaw isOwnerOnlyToolName.
 */
object OwnerOnlyTools {
    private val OWNER_ONLY_TOOL_NAMES = setOf(
        "cron",
        "config_set",
        "config_get",
        "sessions_spawn",
        "sessions_kill"
    )

    fun isOwnerOnlyToolName(name: String): Boolean =
        name in OWNER_ONLY_TOOL_NAMES

    /**
     * Filter tools based on owner status.
     * Aligned with OpenClaw applyOwnerOnlyToolPolicy.
     */
    fun filterByOwnerStatus(
        toolNames: List<String>,
        senderIsOwner: Boolean
    ): List<String> {
        if (senderIsOwner) return toolNames
        return toolNames.filter { !isOwnerOnlyToolName(it) }
    }
}

/**
 * Dangerous tools that should be restricted in certain contexts.
 * Aligned with OpenClaw dangerous-tools.ts.
 */
object DangerousTools {
    /** Tools denied on Gateway HTTP by default */
    val DEFAULT_GATEWAY_HTTP_TOOL_DENY = setOf(
        "sessions_spawn", "sessions_send", "cron", "config_set"
    )

    /** Tools dangerous for ACP (inter-agent) calls */
    val DANGEROUS_ACP_TOOLS = setOf(
        "exec", "sessions_spawn", "sessions_send",
        "config_set", "write_file", "edit_file"
    )
}

/**
 * Subagent tool restrictions.
 * Aligned with OpenClaw subagent tool policy.
 */
object SubagentToolPolicy {
    /** Tools that subagents (non-root agents) should not have access to */
    private val SUBAGENT_RESTRICTED_TOOLS = setOf(
        "cron",
        "config_set",
        "config_get"
    )

    fun filterForSubagent(
        toolNames: List<String>,
        isSubagent: Boolean
    ): List<String> {
        if (!isSubagent) return toolNames
        return toolNames.filter { it !in SUBAGENT_RESTRICTED_TOOLS }
    }
}

/**
 * Resolve tool profile policy (preset tool sets).
 * Aligned with OpenClaw resolveToolProfilePolicy.
 */
fun resolveToolProfilePolicy(profileId: ToolProfileId?): ToolPolicyLike? {
    return when (profileId) {
        ToolProfileId.MINIMAL -> ToolPolicyLike(
            allow = listOf("read_file", "list_dir", "web_search", "web_fetch")
        )
        ToolProfileId.CODING -> ToolPolicyLike(
            allow = listOf("read_file", "write_file", "edit_file", "list_dir", "exec", "web_search", "web_fetch")
        )
        ToolProfileId.MESSAGING -> ToolPolicyLike(
            allow = listOf("read_file", "list_dir", "web_search", "web_fetch",
                "memory_search", "memory_get", "sessions_list", "sessions_history",
                "sessions_send", "tts", "canvas")
        )
        ToolProfileId.FULL, null -> null  // null = no restriction
    }
}

/**
 * ToolPolicyPipeline — Multi-step tool policy pipeline.
 * Aligned with OpenClaw tool-policy-pipeline.ts.
 */
object ToolPolicyPipeline {

    private const val TAG = "ToolPolicyPipeline"

    /**
     * Build default pipeline steps.
     * Aligned with OpenClaw buildDefaultToolPolicyPipelineSteps.
     */
    fun buildDefaultSteps(
        profilePolicy: ToolPolicyLike? = null,
        globalPolicy: ToolPolicyLike? = null,
        agentPolicy: ToolPolicyLike? = null,
        groupPolicy: ToolPolicyLike? = null
    ): List<ToolPolicyPipelineStep> {
        return listOf(
            ToolPolicyPipelineStep(profilePolicy, "profile"),
            ToolPolicyPipelineStep(globalPolicy, "global"),
            ToolPolicyPipelineStep(agentPolicy, "agent"),
            ToolPolicyPipelineStep(groupPolicy, "group")
        )
    }

    /**
     * Apply pipeline: filter tools through ordered steps.
     * Aligned with OpenClaw applyToolPolicyPipeline.
     */
    fun apply(
        toolNames: List<String>,
        steps: List<ToolPolicyPipelineStep>
    ): List<String> {
        var remaining = toolNames

        for (step in steps) {
            val policy = step.policy ?: continue
            remaining = filterByPolicy(remaining, policy)
            if (remaining.isEmpty()) {
                Log.w(TAG, "All tools filtered out at step '${step.label}'")
                break
            }
        }

        return remaining
    }

    /**
     * Filter tool names by a single policy.
     * Aligned with OpenClaw filterToolsByPolicy.
     */
    fun filterByPolicy(toolNames: List<String>, policy: ToolPolicyLike): List<String> {
        var result = toolNames

        // Apply allowlist: keep only allowed tools
        val expandedAllow = ToolGroups.expandToolGroups(policy.allow)
        if (expandedAllow != null) {
            val allowSet = expandedAllow.map { it.lowercase() }.toSet()
            result = result.filter { it.lowercase() in allowSet }
        }

        // Apply denylist: remove denied tools
        val expandedDeny = ToolGroups.expandToolGroups(policy.deny)
        if (expandedDeny != null) {
            val denySet = expandedDeny.map { it.lowercase() }.toSet()
            result = result.filter { it.lowercase() !in denySet }
        }

        return result
    }
}
