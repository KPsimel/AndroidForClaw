package com.xiaomo.androidforclaw.channel

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/channels/draft-stream-controls.ts
 *   (createFinalizableDraftStreamControls, FinalizableDraftStreamState)
 * - ../openclaw/src/channels/draft-stream-loop.ts
 *
 * AndroidForClaw adaptation: streaming draft/typing indicator management.
 * Controls the lifecycle of progressive message updates during LLM streaming.
 */

import com.xiaomo.androidforclaw.logging.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * State for a finalizable draft stream.
 * Aligned with OpenClaw FinalizableDraftStreamState.
 */
data class DraftStreamState(
    var stopped: Boolean = false,
    var final: Boolean = false,
    var messageId: String? = null
)

/**
 * Draft stream controls for progressive message updates.
 * Aligned with OpenClaw createFinalizableDraftStreamControls.
 */
class DraftStreamControls(
    private val throttleMs: Long = 1000L,
    private val sendOrEditMessage: suspend (content: String, messageId: String?) -> String?
) {
    companion object {
        private const val TAG = "DraftStreamControls"
    }

    private val state = DraftStreamState()
    private val mutex = Mutex()
    private var pendingContent: String? = null
    private var throttleJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Update the draft with new content.
     * Throttled to avoid flooding the channel.
     */
    suspend fun update(content: String) = mutex.withLock {
        if (state.stopped || state.final) return@withLock

        pendingContent = content

        if (throttleJob?.isActive != true) {
            throttleJob = scope.launch {
                delay(throttleMs)
                flush()
            }
        }
    }

    /**
     * Flush pending content immediately.
     */
    private suspend fun flush() = mutex.withLock {
        val content = pendingContent ?: return@withLock
        pendingContent = null

        try {
            val newId = sendOrEditMessage(content, state.messageId)
            if (newId != null) {
                state.messageId = newId
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send/edit draft: ${e.message}")
        }
    }

    /**
     * Stop the draft stream and send final content.
     * Aligned with OpenClaw stop().
     */
    suspend fun stop(finalContent: String? = null) = mutex.withLock {
        state.stopped = true
        state.final = true
        throttleJob?.cancel()

        if (finalContent != null) {
            try {
                val newId = sendOrEditMessage(finalContent, state.messageId)
                if (newId != null) {
                    state.messageId = newId
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send final draft: ${e.message}")
            }
        }
    }

    /**
     * Stop for clear (mark stopped, wait for in-flight, don't finalize).
     * Aligned with OpenClaw stopForClear().
     */
    suspend fun stopForClear() = mutex.withLock {
        state.stopped = true
        throttleJob?.cancel()
    }

    /**
     * Get the current message ID (for deletion after stop).
     */
    fun getMessageId(): String? = state.messageId

    /**
     * Check if the stream is stopped.
     */
    fun isStopped(): Boolean = state.stopped

    /**
     * Clean up resources.
     */
    fun dispose() {
        scope.cancel()
    }
}
