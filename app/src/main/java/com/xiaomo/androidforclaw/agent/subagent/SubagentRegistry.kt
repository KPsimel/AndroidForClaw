/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/subagent-registry.ts (in-memory registry, lifecycle listener, completion flow)
 * - ../openclaw/src/agents/subagent-registry-queries.ts (descendant counting, BFS traversal)
 * - ../openclaw/src/agents/subagent-control.ts (resolveControlledSubagentTarget)
 *
 * AndroidForClaw adaptation: ConcurrentHashMap-based registry tracking active/completed subagent runs.
 * Includes target resolution, cascade kill, descendant tracking, and run replacement.
 */
package com.xiaomo.androidforclaw.agent.subagent

import com.xiaomo.androidforclaw.agent.loop.AgentLoop
import com.xiaomo.androidforclaw.logging.Log
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for all subagent runs.
 * Aligned with OpenClaw's in-memory SubagentRunRecord map + query functions.
 */
class SubagentRegistry {
    companion object {
        private const val TAG = "SubagentRegistry"
        /** Auto-archive completed runs after this duration (ms) */
        private const val ARCHIVE_AFTER_MS = 60 * 60 * 1000L  // 1 hour
    }

    /** runId → SubagentRunRecord */
    private val runs = ConcurrentHashMap<String, SubagentRunRecord>()

    /** runId → coroutine Job */
    private val jobs = ConcurrentHashMap<String, Job>()

    /** runId → child AgentLoop (for steer/kill/history) */
    private val agentLoops = ConcurrentHashMap<String, AgentLoop>()

    // ==================== Registration ====================

    fun registerRun(record: SubagentRunRecord, loop: AgentLoop, job: Job) {
        runs[record.runId] = record
        agentLoops[record.runId] = loop
        jobs[record.runId] = job
        Log.i(TAG, "Registered subagent run: ${record.runId} label=${record.label} child=${record.childSessionKey}")
    }

    // ==================== Completion ====================

    fun markCompleted(
        runId: String,
        outcome: SubagentRunOutcome,
        endedReason: SubagentLifecycleEndedReason,
        frozenResult: String?,
    ) {
        val record = runs[runId] ?: return
        record.endedAt = System.currentTimeMillis()
        record.outcome = outcome
        record.endedReason = endedReason
        record.frozenResultText = frozenResult
        // Clean up runtime references
        agentLoops.remove(runId)
        jobs.remove(runId)
        Log.i(TAG, "Completed subagent run: $runId status=${outcome.status} reason=${endedReason}")
    }

    // ==================== Queries ====================

    fun getRunById(runId: String): SubagentRunRecord? = runs[runId]

    fun getAgentLoop(runId: String): AgentLoop? = agentLoops[runId]

    fun getJob(runId: String): Job? = jobs[runId]

    /**
     * Find run by child session key.
     * Returns active run first, fallback to any matching run.
     */
    fun getRunByChildSessionKey(childSessionKey: String): SubagentRunRecord? {
        return runs.values.find { it.childSessionKey == childSessionKey && it.isActive }
            ?: runs.values.find { it.childSessionKey == childSessionKey }
    }

    fun getActiveRunsForParent(parentSessionKey: String): List<SubagentRunRecord> {
        return runs.values.filter { it.requesterSessionKey == parentSessionKey && it.isActive }
    }

    fun getAllRuns(parentSessionKey: String): List<SubagentRunRecord> {
        return runs.values
            .filter { it.requesterSessionKey == parentSessionKey }
            .sortedByDescending { it.createdAt }
    }

    /**
     * Build indexed list: active runs first (sorted by createdAt desc),
     * then completed runs (sorted by endedAt desc).
     * Used for numeric index resolution and list display.
     * Aligned with OpenClaw buildSubagentList ordering.
     */
    fun buildIndexedList(parentSessionKey: String): List<SubagentRunRecord> {
        val allRuns = runs.values.filter { it.requesterSessionKey == parentSessionKey }
        val active = allRuns.filter { it.isActive }.sortedByDescending { it.createdAt }
        val completed = allRuns.filter { !it.isActive }.sortedByDescending { it.endedAt }
        return active + completed
    }

    /**
     * List all runs spawned by a given requester session key (direct children).
     * Used for building child completion findings in announce.
     */
    fun listRunsForRequester(requesterSessionKey: String): List<SubagentRunRecord> {
        return runs.values
            .filter { it.requesterSessionKey == requesterSessionKey }
            .sortedByDescending { it.createdAt }
    }

    fun activeChildCount(parentSessionKey: String): Int {
        return runs.values.count { it.requesterSessionKey == parentSessionKey && it.isActive }
    }

    /**
     * Check if parent can spawn more children.
     * Aligned with OpenClaw active children check in spawnSubagentDirect.
     */
    fun canSpawn(parentSessionKey: String, maxChildren: Int): Boolean {
        return activeChildCount(parentSessionKey) < maxChildren
    }

    // ==================== Target Resolution ====================

    /**
     * Resolve a target token to a SubagentRunRecord.
     * Resolution order aligned with OpenClaw resolveControlledSubagentTarget:
     * 1. "last" keyword → most recently started active run (or most recent)
     * 2. Numeric index → 1-based index into buildIndexedList
     * 3. Contains ":" → session key exact match
     * 4. Exact label match (case-insensitive)
     * 5. Label prefix match (case-insensitive)
     * 6. RunId prefix match
     */
    fun resolveTarget(token: String, parentSessionKey: String): SubagentRunRecord? {
        if (token.isBlank()) return null

        val parentRuns = getAllRuns(parentSessionKey)
        if (parentRuns.isEmpty()) return null

        // 1. "last" keyword
        if (token.equals("last", ignoreCase = true)) {
            return parentRuns.firstOrNull { it.isActive }
                ?: parentRuns.firstOrNull()
        }

        // 2. Numeric index (1-based)
        token.toIntOrNull()?.let { index ->
            val indexed = buildIndexedList(parentSessionKey)
            return indexed.getOrNull(index - 1)
        }

        // 3. Session key (contains ":")
        if (":" in token) {
            return parentRuns.find { it.childSessionKey == token }
        }

        // 4. Exact label match (case-insensitive)
        val exactLabel = parentRuns.filter { it.label.equals(token, ignoreCase = true) }
        if (exactLabel.size == 1) return exactLabel[0]

        // 5. Label prefix match (case-insensitive)
        val prefixLabel = parentRuns.filter { it.label.startsWith(token, ignoreCase = true) }
        if (prefixLabel.size == 1) return prefixLabel[0]

        // 6. RunId prefix match
        val prefixRunId = parentRuns.filter { it.runId.startsWith(token) }
        if (prefixRunId.size == 1) return prefixRunId[0]

        return null
    }

    // ==================== Descendant Tracking ====================

    /**
     * Count pending (active) descendant runs for a given session key.
     * BFS traversal through the spawn tree.
     * Aligned with OpenClaw countPendingDescendantRunsFromRuns.
     */
    fun countPendingDescendantRuns(sessionKey: String): Int {
        var count = 0
        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        queue.add(sessionKey)
        visited.add(sessionKey)

        while (queue.isNotEmpty()) {
            val currentKey = queue.removeFirst()
            val children = runs.values.filter { it.requesterSessionKey == currentKey }
            for (child in children) {
                if (child.isActive) count++
                if (child.childSessionKey !in visited) {
                    visited.add(child.childSessionKey)
                    queue.add(child.childSessionKey)
                }
            }
        }
        return count
    }

    // ==================== Control ====================

    /**
     * Kill a running subagent by cancelling its coroutine Job.
     * Aligned with OpenClaw killControlledSubagentRun (single target).
     */
    fun killRun(runId: String): Boolean {
        val job = jobs[runId] ?: return false
        val record = runs[runId] ?: return false
        if (!record.isActive) return false

        Log.i(TAG, "Killing subagent run: $runId")
        job.cancel()

        markCompleted(
            runId,
            SubagentRunOutcome(SubagentRunStatus.ERROR, "Killed by parent"),
            SubagentLifecycleEndedReason.SUBAGENT_KILLED,
            frozenResult = null,
        )
        return true
    }

    /**
     * Cascade kill: kill a run and all its descendants (BFS).
     * Aligned with OpenClaw cascadeKillChildren.
     * Returns list of killed runIds.
     */
    fun cascadeKill(runId: String): List<String> {
        val killed = mutableListOf<String>()
        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        queue.add(runId)

        while (queue.isNotEmpty()) {
            val currentRunId = queue.removeFirst()
            if (currentRunId in visited) continue
            visited.add(currentRunId)

            val record = runs[currentRunId] ?: continue

            // Find children of this run's session
            val children = runs.values.filter {
                it.requesterSessionKey == record.childSessionKey && it.isActive
            }
            for (child in children) {
                queue.add(child.runId)
            }

            // Kill this run if still active
            if (record.isActive) {
                val job = jobs[currentRunId]
                job?.cancel()
                markCompleted(
                    currentRunId,
                    SubagentRunOutcome(SubagentRunStatus.ERROR, "Killed by parent (cascade)"),
                    SubagentLifecycleEndedReason.SUBAGENT_KILLED,
                    frozenResult = null,
                )
                killed.add(currentRunId)
            }
        }

        if (killed.isNotEmpty()) {
            Log.i(TAG, "Cascade killed ${killed.size} runs starting from $runId")
        }
        return killed
    }

    // ==================== Run Replacement (Steer Restart) ====================

    /**
     * Replace a run record with a new one after steer restart.
     * Old run stays in registry for history; new run takes over runtime references.
     * Aligned with OpenClaw replaceSubagentRunAfterSteer.
     */
    fun replaceRun(oldRunId: String, newRecord: SubagentRunRecord, loop: AgentLoop, job: Job) {
        // Remove old runtime references (record stays for history)
        agentLoops.remove(oldRunId)
        jobs.remove(oldRunId)

        // Register new run
        runs[newRecord.runId] = newRecord
        agentLoops[newRecord.runId] = loop
        jobs[newRecord.runId] = job
        Log.i(TAG, "Replaced run: $oldRunId → ${newRecord.runId} (session ${newRecord.childSessionKey})")
    }

    // ==================== Cleanup ====================

    /**
     * Remove archived (completed + old) runs.
     * Called periodically or after announce.
     */
    fun sweepArchived() {
        val now = System.currentTimeMillis()
        val toRemove = runs.values.filter { record ->
            !record.isActive && record.endedAt != null && (now - record.endedAt!!) > ARCHIVE_AFTER_MS
        }
        for (record in toRemove) {
            runs.remove(record.runId)
            Log.d(TAG, "Archived subagent run: ${record.runId}")
        }
        if (toRemove.isNotEmpty()) {
            Log.i(TAG, "Swept ${toRemove.size} archived subagent runs")
        }
    }
}
