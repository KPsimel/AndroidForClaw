/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/subagent-spawn.ts (spawnSubagentDirect)
 * - ../openclaw/src/agents/subagent-announce.ts (runSubagentAnnounceFlow, announceToParent)
 * - ../openclaw/src/agents/subagent-control.ts (killControlledSubagentRun, steerControlledSubagentRun)
 *
 * AndroidForClaw adaptation: in-process coroutine-based subagent spawning.
 * Replaces OpenClaw's gateway WebSocket communication with direct steerChannel injection.
 */
package com.xiaomo.androidforclaw.agent.subagent

import com.xiaomo.androidforclaw.agent.context.ContextManager
import com.xiaomo.androidforclaw.agent.loop.AgentLoop
import com.xiaomo.androidforclaw.agent.tools.AndroidToolRegistry
import com.xiaomo.androidforclaw.agent.tools.SessionsHistoryTool
import com.xiaomo.androidforclaw.agent.tools.SessionsKillTool
import com.xiaomo.androidforclaw.agent.tools.SessionsListTool
import com.xiaomo.androidforclaw.agent.tools.SessionsSendTool
import com.xiaomo.androidforclaw.agent.tools.SessionsSpawnTool
import com.xiaomo.androidforclaw.agent.tools.SessionsYieldTool
import com.xiaomo.androidforclaw.agent.tools.Tool
import com.xiaomo.androidforclaw.agent.tools.ToolRegistry
import com.xiaomo.androidforclaw.config.ConfigLoader
import com.xiaomo.androidforclaw.config.SubagentsConfig
import com.xiaomo.androidforclaw.logging.Log
import com.xiaomo.androidforclaw.providers.UnifiedLLMProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Core subagent spawner — validates, creates, and manages subagent AgentLoop instances.
 * Aligned with OpenClaw spawnSubagentDirect + announce flow + control operations.
 *
 * Android-specific: subagents run as in-process coroutines with direct steerChannel communication
 * (no gateway WebSocket).
 */
class SubagentSpawner(
    val registry: SubagentRegistry,
    private val configLoader: ConfigLoader,
    private val llmProvider: UnifiedLLMProvider,
    private val toolRegistry: ToolRegistry,
    private val androidToolRegistry: AndroidToolRegistry,
) {
    companion object {
        private const val TAG = "SubagentSpawner"

        /** Announce retry delays in ms (aligned with OpenClaw: 5s, 10s, 20s) */
        private val ANNOUNCE_RETRY_DELAYS = longArrayOf(5_000, 10_000, 20_000)

        /** Steer abort settle wait (2s on Android vs 5s on OpenClaw server) */
        private const val STEER_SETTLE_WAIT_MS = 2000L

        /**
         * Build the set of subagent tools for a given parent session.
         * LEAF agents get no subagent tools (they cannot spawn).
         * Aligned with OpenClaw per-session tool injection.
         */
        fun buildSubagentTools(
            spawner: SubagentSpawner,
            parentSessionKey: String,
            parentAgentLoop: AgentLoop,
            parentDepth: Int,
        ): List<Tool> {
            return listOf(
                SessionsSpawnTool(spawner, parentSessionKey, parentAgentLoop, parentDepth),
                SessionsListTool(spawner.registry, parentSessionKey),
                SessionsSendTool(spawner, parentSessionKey, parentAgentLoop),
                SessionsKillTool(spawner, parentSessionKey),
                SessionsHistoryTool(spawner.registry, parentSessionKey),
                SessionsYieldTool(parentAgentLoop),
            )
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Rate limit: last steer time per (caller, target) pair. Aligned with OpenClaw steerRateLimit Map. */
    private val lastSteerTime = ConcurrentHashMap<String, Long>()

    // ==================== Spawn ====================

    /**
     * Spawn a subagent.
     * Aligned with OpenClaw spawnSubagentDirect validation + launch flow.
     */
    suspend fun spawn(
        params: SpawnSubagentParams,
        parentSessionKey: String,
        parentAgentLoop: AgentLoop,
        parentDepth: Int,
    ): SpawnSubagentResult {
        val config = try {
            configLoader.loadOpenClawConfig().agents?.defaults?.subagents ?: SubagentsConfig()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load subagents config, using defaults: ${e.message}")
            SubagentsConfig()
        }

        if (!config.enabled) {
            return SpawnSubagentResult(
                status = SpawnStatus.FORBIDDEN,
                note = "Subagents are disabled in configuration."
            )
        }

        // 1. Depth check (aligned with OpenClaw: callerDepth >= maxSpawnDepth → forbidden)
        val childDepth = parentDepth + 1
        if (parentDepth >= config.maxSpawnDepth) {
            return SpawnSubagentResult(
                status = SpawnStatus.FORBIDDEN,
                note = "Maximum spawn depth (${config.maxSpawnDepth}) reached. Cannot spawn at depth $childDepth."
            )
        }

        // 2. Active children check (aligned with OpenClaw: activeChildren >= maxChildrenPerAgent → forbidden)
        if (!registry.canSpawn(parentSessionKey, config.maxChildrenPerAgent)) {
            return SpawnSubagentResult(
                status = SpawnStatus.FORBIDDEN,
                note = "Maximum concurrent children (${config.maxChildrenPerAgent}) reached for this session."
            )
        }

        // 3. Generate identifiers
        val runId = UUID.randomUUID().toString()
        val childSessionKey = "agent:main:subagent:$runId"

        // 4. Resolve model
        val model = params.model ?: config.model
            ?: try { configLoader.loadOpenClawConfig().resolveDefaultModel() } catch (_: Exception) { null }

        // 5. Resolve capabilities for child
        val childCapabilities = resolveSubagentCapabilities(childDepth, config.maxSpawnDepth)

        // 6. Build system prompt
        val label = params.label ?: params.task.take(40).replace('\n', ' ')
        val systemPrompt = SubagentPromptBuilder.build(
            task = params.task,
            label = label,
            capabilities = childCapabilities,
            parentSessionKey = parentSessionKey,
            childSessionKey = childSessionKey,
        )

        // 7. Create child AgentLoop
        val childContextManager = ContextManager(llmProvider)
        val childLoop = AgentLoop(
            llmProvider = llmProvider,
            toolRegistry = toolRegistry,
            androidToolRegistry = androidToolRegistry,
            contextManager = childContextManager,
            modelRef = model,
            configLoader = configLoader,
        )

        // 8. If child can spawn (ORCHESTRATOR), inject subagent tools
        if (childCapabilities.canSpawn) {
            val childSubagentTools = buildSubagentTools(
                spawner = this,
                parentSessionKey = childSessionKey,
                parentAgentLoop = childLoop,
                parentDepth = childDepth,
            )
            childLoop.extraTools = childSubagentTools
        }

        // 9. Create run record
        val timeoutSeconds = params.runTimeoutSeconds ?: config.defaultTimeoutSeconds
        val record = SubagentRunRecord(
            runId = runId,
            childSessionKey = childSessionKey,
            requesterSessionKey = parentSessionKey,
            task = params.task,
            label = label,
            model = model,
            cleanup = params.cleanup,
            spawnMode = params.mode,
            createdAt = System.currentTimeMillis(),
            runTimeoutSeconds = timeoutSeconds,
            depth = childDepth,
        )

        // 10. Timeout
        val timeoutMs = if (timeoutSeconds > 0) timeoutSeconds * 1000L else 0L

        // 11. Launch child coroutine
        val job = scope.launch {
            record.startedAt = System.currentTimeMillis()
            Log.i(TAG, "Subagent started: $runId label=$label model=$model timeout=${timeoutSeconds}s")

            try {
                val result = if (timeoutMs > 0) {
                    withTimeoutOrNull(timeoutMs) {
                        childLoop.run(
                            systemPrompt = systemPrompt,
                            userMessage = params.task,
                            reasoningEnabled = true,
                        )
                    }
                } else {
                    childLoop.run(
                        systemPrompt = systemPrompt,
                        userMessage = params.task,
                        reasoningEnabled = true,
                    )
                }

                if (result == null) {
                    // Timeout
                    Log.w(TAG, "Subagent timed out: $runId after ${timeoutSeconds}s")
                    childLoop.stop()
                    val outcome = SubagentRunOutcome(SubagentRunStatus.TIMEOUT, "Timed out after ${timeoutSeconds}s")
                    record.frozenResultText = null
                    registry.markCompleted(runId, outcome, SubagentLifecycleEndedReason.SUBAGENT_ERROR, null)
                    announceToParent(parentAgentLoop, record, outcome)
                } else {
                    // Success
                    val outcome = SubagentRunOutcome(SubagentRunStatus.OK)
                    record.frozenResultText = result.finalContent
                    registry.markCompleted(runId, outcome, SubagentLifecycleEndedReason.SUBAGENT_COMPLETE, result.finalContent)
                    announceToParent(parentAgentLoop, record, outcome)
                    Log.i(TAG, "Subagent completed: $runId iterations=${result.iterations} tools=${result.toolsUsed.size}")
                }

                // Check if any ancestor needs waking after this completion
                checkDescendantSettle(record, parentAgentLoop)

            } catch (e: kotlinx.coroutines.CancellationException) {
                // Killed by parent or steer restart
                Log.i(TAG, "Subagent cancelled: $runId")
                val outcome = SubagentRunOutcome(SubagentRunStatus.ERROR, "Killed by parent")
                registry.markCompleted(runId, outcome, SubagentLifecycleEndedReason.SUBAGENT_KILLED, null)
                announceToParent(parentAgentLoop, record, outcome)
            } catch (e: Exception) {
                Log.e(TAG, "Subagent error: $runId", e)
                val outcome = SubagentRunOutcome(SubagentRunStatus.ERROR, e.message ?: "Unknown error")
                registry.markCompleted(runId, outcome, SubagentLifecycleEndedReason.SUBAGENT_ERROR, null)
                announceToParent(parentAgentLoop, record, outcome)
            }
        }

        // 12. Register in registry
        registry.registerRun(record, childLoop, job)

        Log.i(TAG, "Subagent spawned: $runId → $childSessionKey depth=$childDepth role=${childCapabilities.role}")

        return SpawnSubagentResult(
            status = SpawnStatus.ACCEPTED,
            childSessionKey = childSessionKey,
            runId = runId,
            mode = params.mode,
            note = SPAWN_ACCEPTED_NOTE,
            modelApplied = model,
        )
    }

    // ==================== Announce ====================

    /**
     * Announce subagent completion to parent via steerChannel.
     * Aligned with OpenClaw runSubagentAnnounceFlow:
     * 1. Check pending descendants → defer if > 0
     * 2. Collect child completion findings
     * 3. Retry with exponential backoff (5s, 10s, 20s)
     * 4. Complete parent's yield signal if present
     */
    private suspend fun announceToParent(
        parentAgentLoop: AgentLoop,
        record: SubagentRunRecord,
        outcome: SubagentRunOutcome,
    ) {
        // 1. Check pending descendants — if > 0, defer announce
        val pendingDescendants = registry.countPendingDescendantRuns(record.childSessionKey)
        if (pendingDescendants > 0) {
            Log.i(TAG, "Deferring announce for ${record.runId}: $pendingDescendants pending descendants")
            record.suppressAnnounceReason = "pending_descendants:$pendingDescendants"
            record.wakeOnDescendantSettle = true
            return
        }

        // 2. Collect child completion findings
        val children = registry.listRunsForRequester(record.childSessionKey)
        val findings = SubagentPromptBuilder.buildChildCompletionFindings(children)

        // 3. Build announcement
        val announcement = SubagentPromptBuilder.buildAnnouncement(record, outcome, findings)

        // 4. Retry with exponential backoff (aligned with OpenClaw: 3 retries, delays 5s/10s/20s)
        var sent = false
        for (attempt in 0..ANNOUNCE_RETRY_DELAYS.size) {
            val result = parentAgentLoop.steerChannel.trySend(announcement)
            if (result.isSuccess) {
                sent = true
                record.announceRetryCount = attempt
                Log.i(TAG, "Announced ${record.runId} to parent (attempt ${attempt + 1})")
                break
            }

            record.lastAnnounceRetryAt = System.currentTimeMillis()
            if (attempt < ANNOUNCE_RETRY_DELAYS.size) {
                val delayMs = ANNOUNCE_RETRY_DELAYS[attempt]
                Log.w(TAG, "Announce retry ${attempt + 1}/${ANNOUNCE_RETRY_DELAYS.size} for ${record.runId}, waiting ${delayMs}ms")
                delay(delayMs)
            }
        }

        if (!sent) {
            Log.e(TAG, "Failed to announce ${record.runId} after ${ANNOUNCE_RETRY_DELAYS.size + 1} attempts")
            record.suppressAnnounceReason = "channel_full_after_retries"
        }

        // 5. Complete parent's yield signal if present (sessions_yield)
        parentAgentLoop.yieldSignal?.let { deferred ->
            if (!deferred.isCompleted) {
                deferred.complete(announcement)
                Log.i(TAG, "Completed yield signal for parent after announcing ${record.runId}")
            }
        }

        // Sweep old archived runs
        registry.sweepArchived()
    }

    /**
     * Check if any ancestor has wakeOnDescendantSettle and all descendants
     * are now settled. If so, re-announce the ancestor.
     * Called after every run completion.
     * Aligned with OpenClaw descendant settle wake logic.
     */
    private suspend fun checkDescendantSettle(
        completedRecord: SubagentRunRecord,
        parentAgentLoop: AgentLoop,
    ) {
        // Walk up: find the parent run that spawned the completed child
        val parentRun = registry.getRunByChildSessionKey(completedRecord.requesterSessionKey) ?: return

        if (!parentRun.wakeOnDescendantSettle) return

        val remaining = registry.countPendingDescendantRuns(parentRun.childSessionKey)
        if (remaining > 0) {
            Log.d(TAG, "Parent ${parentRun.runId} still has $remaining pending descendants")
            return
        }

        // All descendants settled — announce the parent now
        Log.i(TAG, "All descendants settled for ${parentRun.runId}, triggering deferred announce")
        parentRun.wakeOnDescendantSettle = false
        parentRun.suppressAnnounceReason = null

        val outcome = parentRun.outcome ?: SubagentRunOutcome(SubagentRunStatus.OK)
        announceToParent(parentAgentLoop, parentRun, outcome)
    }

    // ==================== Control Operations ====================

    /**
     * Kill a running subagent, optionally with cascade.
     * Aligned with OpenClaw killControlledSubagentRun + cascadeKillChildren.
     *
     * @return Pair of (success, list of killed runIds)
     */
    fun kill(runId: String, cascade: Boolean = false): Pair<Boolean, List<String>> {
        return if (cascade) {
            val killed = registry.cascadeKill(runId)
            Pair(killed.isNotEmpty(), killed)
        } else {
            val success = registry.killRun(runId)
            Pair(success, if (success) listOf(runId) else emptyList())
        }
    }

    /**
     * Steer a running subagent: abort current run and restart with new message.
     * Aligned with OpenClaw steerControlledSubagentRun (abort + restart semantics).
     *
     * Flow:
     * 1. Rate limit check (2s per caller-target pair)
     * 2. Cancel the child coroutine Job (abort)
     * 3. Clear steer channel
     * 4. Wait for abort to settle (best-effort)
     * 5. Reset AgentLoop internal state
     * 6. Accumulate old runtime
     * 7. Mark old run completed
     * 8. Create new run record (preserving session key)
     * 9. Launch new run() with steer message
     * 10. Replace run record in registry
     */
    suspend fun steer(
        runId: String,
        message: String,
        callerSessionKey: String,
        parentAgentLoop: AgentLoop,
    ): Pair<Boolean, String?> {
        val record = registry.getRunById(runId) ?: return Pair(false, "Run not found: $runId")
        if (!record.isActive) return Pair(false, "Run already completed: $runId")
        val childLoop = registry.getAgentLoop(runId) ?: return Pair(false, "AgentLoop not found for: $runId")
        val job = registry.getJob(runId) ?: return Pair(false, "Job not found for: $runId")

        // 1. Rate limit check (2s, aligned with OpenClaw STEER_RATE_LIMIT_MS)
        val rateKey = "$callerSessionKey:$runId"
        val now = System.currentTimeMillis()
        val lastTime = lastSteerTime[rateKey]
        if (lastTime != null && (now - lastTime) < STEER_RATE_LIMIT_MS) {
            val waitMs = STEER_RATE_LIMIT_MS - (now - lastTime)
            return Pair(false, "Rate limited: wait ${waitMs}ms")
        }
        lastSteerTime[rateKey] = now

        // 2. Cancel the child coroutine Job (abort)
        Log.i(TAG, "Steer: aborting run $runId for restart")
        job.cancel()

        // 3. Clear steer channel
        while (childLoop.steerChannel.tryReceive().isSuccess) { /* drain */ }

        // 4. Wait for abort to settle (best-effort)
        try {
            delay(STEER_SETTLE_WAIT_MS)
        } catch (_: Exception) { }

        // 5. Reset AgentLoop state
        childLoop.reset()

        // 6. Accumulate runtime from old run
        val oldRuntimeMs = record.runtimeMs

        // 7. Mark old run as completed (steer-restarted)
        registry.markCompleted(
            runId,
            SubagentRunOutcome(SubagentRunStatus.OK, "Steered (restarted)"),
            SubagentLifecycleEndedReason.SUBAGENT_COMPLETE,
            frozenResult = null,
        )

        // 8. Create new run record (preserving session key)
        val newRunId = UUID.randomUUID().toString()
        val newRecord = SubagentRunRecord(
            runId = newRunId,
            childSessionKey = record.childSessionKey,
            requesterSessionKey = record.requesterSessionKey,
            task = message,
            label = record.label,
            model = record.model,
            cleanup = record.cleanup,
            spawnMode = record.spawnMode,
            createdAt = System.currentTimeMillis(),
            accumulatedRuntimeMs = oldRuntimeMs,
            runTimeoutSeconds = record.runTimeoutSeconds,
            depth = record.depth,
        )

        // 9. Rebuild system prompt
        val config = try {
            configLoader.loadOpenClawConfig().agents?.defaults?.subagents ?: SubagentsConfig()
        } catch (_: Exception) { SubagentsConfig() }
        val childCapabilities = resolveSubagentCapabilities(record.depth, config.maxSpawnDepth)
        val systemPrompt = SubagentPromptBuilder.build(
            task = message,
            label = record.label,
            capabilities = childCapabilities,
            parentSessionKey = record.requesterSessionKey,
            childSessionKey = record.childSessionKey,
        )

        // 10. Launch new coroutine with conversation context from previous run
        val timeoutMs = (record.runTimeoutSeconds ?: config.defaultTimeoutSeconds).let {
            if (it > 0) it * 1000L else 0L
        }
        val previousMessages = childLoop.conversationMessages.toList()

        val newJob = scope.launch {
            newRecord.startedAt = System.currentTimeMillis()
            Log.i(TAG, "Steer restart: $newRunId (was $runId)")

            try {
                val result = if (timeoutMs > 0) {
                    withTimeoutOrNull(timeoutMs) {
                        childLoop.run(
                            systemPrompt = systemPrompt,
                            userMessage = message,
                            contextHistory = previousMessages.drop(1), // Skip system prompt
                            reasoningEnabled = true,
                        )
                    }
                } else {
                    childLoop.run(
                        systemPrompt = systemPrompt,
                        userMessage = message,
                        contextHistory = previousMessages.drop(1),
                        reasoningEnabled = true,
                    )
                }

                if (result == null) {
                    val outcome = SubagentRunOutcome(SubagentRunStatus.TIMEOUT)
                    registry.markCompleted(newRunId, outcome, SubagentLifecycleEndedReason.SUBAGENT_ERROR, null)
                    announceToParent(parentAgentLoop, newRecord, outcome)
                } else {
                    val outcome = SubagentRunOutcome(SubagentRunStatus.OK)
                    newRecord.frozenResultText = result.finalContent
                    registry.markCompleted(newRunId, outcome, SubagentLifecycleEndedReason.SUBAGENT_COMPLETE, result.finalContent)
                    announceToParent(parentAgentLoop, newRecord, outcome)
                }

                checkDescendantSettle(newRecord, parentAgentLoop)
            } catch (e: kotlinx.coroutines.CancellationException) {
                val outcome = SubagentRunOutcome(SubagentRunStatus.ERROR, "Killed")
                registry.markCompleted(newRunId, outcome, SubagentLifecycleEndedReason.SUBAGENT_KILLED, null)
                announceToParent(parentAgentLoop, newRecord, outcome)
            } catch (e: Exception) {
                val outcome = SubagentRunOutcome(SubagentRunStatus.ERROR, e.message)
                registry.markCompleted(newRunId, outcome, SubagentLifecycleEndedReason.SUBAGENT_ERROR, null)
                announceToParent(parentAgentLoop, newRecord, outcome)
            }
        }

        // 11. Replace in registry
        registry.replaceRun(runId, newRecord, childLoop, newJob)

        Log.i(TAG, "Steer complete: $runId → $newRunId")
        return Pair(true, "Steered: run restarted as $newRunId")
    }
}
