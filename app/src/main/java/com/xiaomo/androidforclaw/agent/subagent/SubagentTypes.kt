/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/subagent-registry.types.ts (SubagentRunRecord)
 * - ../openclaw/src/agents/subagent-capabilities.ts (role, control scope, depth)
 * - ../openclaw/src/agents/subagent-lifecycle-events.ts (ended reason, ended outcome)
 * - ../openclaw/src/agents/subagent-spawn.ts (SpawnSubagentParams, SpawnSubagentResult, SpawnSubagentMode)
 * - ../openclaw/src/agents/subagent-announce.ts (SubagentRunOutcome)
 */
package com.xiaomo.androidforclaw.agent.subagent

// ==================== Spawn Mode ====================

/** Aligned with OpenClaw SpawnSubagentMode ("run" | "session") */
enum class SpawnMode {
    /** One-shot: run task, announce result, clean up */
    RUN,
    /** Persistent: thread-bound session stays active after task */
    SESSION;

    val wireValue: String get() = name.lowercase()
}

// ==================== Spawn Status ====================

/** Aligned with OpenClaw SpawnSubagentResult.status */
enum class SpawnStatus {
    ACCEPTED,
    FORBIDDEN,
    ERROR;

    val wireValue: String get() = name.lowercase()
}

// ==================== Role System ====================

/**
 * Aligned with OpenClaw SubagentSessionRole.
 * Determined by spawn depth relative to maxSpawnDepth.
 */
enum class SubagentSessionRole {
    /** Depth 0: top-level agent. Can spawn children, can control children. */
    MAIN,
    /** 0 < depth < maxSpawnDepth: intermediate. Can spawn and control children. */
    ORCHESTRATOR,
    /** depth >= maxSpawnDepth: leaf worker. Cannot spawn further subagents. */
    LEAF;

    val wireValue: String get() = name.lowercase()
}

/**
 * Aligned with OpenClaw SubagentControlScope.
 * MAIN/ORCHESTRATOR get CHILDREN, LEAF gets NONE.
 */
enum class SubagentControlScope {
    CHILDREN,
    NONE;

    val wireValue: String get() = name.lowercase()
}

// ==================== Lifecycle ====================

/** Aligned with OpenClaw SubagentLifecycleEndedReason */
enum class SubagentLifecycleEndedReason {
    SUBAGENT_COMPLETE,
    SUBAGENT_ERROR,
    SUBAGENT_KILLED,
    SESSION_RESET,
    SESSION_DELETE;

    val wireValue: String get() = name.lowercase().replace('_', '-')
}

/** Aligned with OpenClaw SubagentRunOutcome.status */
enum class SubagentRunStatus {
    OK,
    ERROR,
    TIMEOUT,
    UNKNOWN;

    val wireValue: String get() = name.lowercase()
}

// ==================== Data Classes ====================

/** Aligned with OpenClaw SubagentRunOutcome */
data class SubagentRunOutcome(
    val status: SubagentRunStatus,
    val error: String? = null,
)

/**
 * Aligned with OpenClaw SubagentRunRecord.
 * Tracks a single subagent run from spawn to completion/cleanup.
 */
data class SubagentRunRecord(
    val runId: String,
    val childSessionKey: String,
    val requesterSessionKey: String,
    val task: String,
    val label: String,
    val model: String?,
    val cleanup: Boolean,
    val spawnMode: SpawnMode,
    val createdAt: Long,
    var startedAt: Long? = null,
    var endedAt: Long? = null,
    var outcome: SubagentRunOutcome? = null,
    var frozenResultText: String? = null,
    var endedReason: SubagentLifecycleEndedReason? = null,
    // --- Announce retry tracking (aligned with OpenClaw announceRetryCount/lastAnnounceRetryAt) ---
    var suppressAnnounceReason: String? = null,
    var announceRetryCount: Int = 0,
    var lastAnnounceRetryAt: Long? = null,
    // --- Descendant tracking (aligned with OpenClaw wakeOnDescendantSettle) ---
    var wakeOnDescendantSettle: Boolean = false,
    // --- Steer restart: accumulated runtime from previous runs ---
    var accumulatedRuntimeMs: Long = 0L,
    // --- Timeout config preservation across steer restarts ---
    var runTimeoutSeconds: Int? = null,
    // --- Depth: stored for steer restart prompt rebuilding ---
    val depth: Int = 0,
) {
    val isActive: Boolean get() = endedAt == null
    val runtimeMs: Long get() {
        val start = startedAt ?: return accumulatedRuntimeMs
        val end = endedAt ?: System.currentTimeMillis()
        return (end - start) + accumulatedRuntimeMs
    }
}

/** Aligned with OpenClaw SpawnSubagentParams */
data class SpawnSubagentParams(
    val task: String,
    val label: String? = null,
    val agentId: String? = null,
    val model: String? = null,
    val thinking: String? = null,
    val runTimeoutSeconds: Int? = null,
    val mode: SpawnMode = SpawnMode.RUN,
    val cleanup: Boolean = true,
)

/** Aligned with OpenClaw SpawnSubagentResult */
data class SpawnSubagentResult(
    val status: SpawnStatus,
    val childSessionKey: String? = null,
    val runId: String? = null,
    val mode: SpawnMode? = null,
    val note: String? = null,
    val error: String? = null,
    val modelApplied: String? = null,
)

/** Resolved capabilities for a given depth. Aligned with OpenClaw resolveSubagentCapabilities return. */
data class SubagentCapabilities(
    val depth: Int,
    val role: SubagentSessionRole,
    val controlScope: SubagentControlScope,
    val canSpawn: Boolean,
    val canControlChildren: Boolean,
)

// ==================== Capability Resolution ====================

/** Default max spawn depth (aligned with OpenClaw DEFAULT_SUBAGENT_MAX_SPAWN_DEPTH = 1) */
const val DEFAULT_MAX_SPAWN_DEPTH = 1

/** Default max concurrent children per parent (aligned with OpenClaw) */
const val DEFAULT_MAX_CHILDREN_PER_AGENT = 5

/** Steer rate limit: minimum interval between steers on the same caller-target pair (aligned with OpenClaw STEER_RATE_LIMIT_MS) */
const val STEER_RATE_LIMIT_MS = 2000L

/**
 * Aligned with OpenClaw resolveSubagentRoleForDepth.
 * depth <= 0 → MAIN, depth < maxSpawnDepth → ORCHESTRATOR, else → LEAF.
 */
fun resolveSubagentRole(depth: Int, maxSpawnDepth: Int = DEFAULT_MAX_SPAWN_DEPTH): SubagentSessionRole {
    return when {
        depth <= 0 -> SubagentSessionRole.MAIN
        depth < maxSpawnDepth -> SubagentSessionRole.ORCHESTRATOR
        else -> SubagentSessionRole.LEAF
    }
}

/** Aligned with OpenClaw resolveSubagentControlScopeForRole */
fun resolveSubagentControlScope(role: SubagentSessionRole): SubagentControlScope {
    return if (role == SubagentSessionRole.LEAF) SubagentControlScope.NONE else SubagentControlScope.CHILDREN
}

/** Aligned with OpenClaw resolveSubagentCapabilities */
fun resolveSubagentCapabilities(depth: Int, maxSpawnDepth: Int = DEFAULT_MAX_SPAWN_DEPTH): SubagentCapabilities {
    val role = resolveSubagentRole(depth, maxSpawnDepth)
    val controlScope = resolveSubagentControlScope(role)
    return SubagentCapabilities(
        depth = depth,
        role = role,
        controlScope = controlScope,
        canSpawn = role == SubagentSessionRole.MAIN || role == SubagentSessionRole.ORCHESTRATOR,
        canControlChildren = controlScope == SubagentControlScope.CHILDREN,
    )
}

/** Note returned to LLM after successful spawn (aligned with OpenClaw SUBAGENT_SPAWN_ACCEPTED_NOTE) */
const val SPAWN_ACCEPTED_NOTE = "Auto-announce is push-based. After spawning children, do NOT call sessions_list, sessions_history, exec sleep, or any polling tool. Wait for completion events to arrive as user messages, track expected child session keys, and only send your final answer after ALL expected completions arrive. If a child completion event arrives AFTER your final answer, reply ONLY with NO_REPLY."
