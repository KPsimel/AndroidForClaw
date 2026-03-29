package com.xiaomo.androidforclaw.hooks

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/hooks/internal-hooks.ts
 *   (InternalHookEvent, registerInternalHook, triggerInternalHook, clearInternalHooks)
 *
 * AndroidForClaw adaptation: event-driven hook system for agent lifecycle events.
 */

import com.xiaomo.androidforclaw.logging.Log
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Internal hook event types.
 * Aligned with OpenClaw InternalHookEventType.
 */
enum class InternalHookEventType {
    COMMAND,
    SESSION,
    AGENT,
    GATEWAY,
    MESSAGE
}

/**
 * Internal hook event.
 * Aligned with OpenClaw InternalHookEvent.
 */
data class InternalHookEvent(
    val type: InternalHookEventType,
    val action: String,
    val sessionKey: String? = null,
    val context: Map<String, Any?> = emptyMap(),
    val timestamp: Date = Date(),
    val messages: List<String> = emptyList()
)

/**
 * Hook handler function type.
 * Aligned with OpenClaw InternalHookHandler.
 */
typealias InternalHookHandler = suspend (InternalHookEvent) -> Unit

/**
 * Agent bootstrap hook context.
 * Aligned with OpenClaw AgentBootstrapHookContext.
 */
data class AgentBootstrapHookContext(
    val workspaceDir: String,
    val bootstrapFiles: List<String>,
    val sessionKey: String?
)

/**
 * Gateway startup hook context.
 * Aligned with OpenClaw GatewayStartupHookContext.
 */
data class GatewayStartupHookContext(
    val port: Int,
    val bindAddress: String
)

/**
 * Message received hook context.
 * Aligned with OpenClaw MessageReceivedHookContext.
 */
data class MessageReceivedHookContext(
    val senderId: String?,
    val senderName: String?,
    val content: String,
    val channel: String,
    val chatType: String?,
    val conversationId: String?
)

/**
 * Message sent hook context.
 * Aligned with OpenClaw MessageSentHookContext.
 */
data class MessageSentHookContext(
    val content: String,
    val channel: String,
    val chatType: String?,
    val conversationId: String?
)

/**
 * InternalHooks — Event-driven hook system.
 * Aligned with OpenClaw internal-hooks.ts.
 *
 * Handlers are registered by event key (type or type:action).
 * Multiple handlers can be registered per key and fire in order.
 */
object InternalHooks {

    private const val TAG = "InternalHooks"

    /** Handler registry: eventKey → list of handlers */
    private val handlers = ConcurrentHashMap<String, MutableList<InternalHookHandler>>()

    /**
     * Register a hook handler.
     * Aligned with OpenClaw registerInternalHook.
     *
     * @param eventKey The event key: "type" or "type:action"
     * @param handler The handler function
     */
    fun register(eventKey: String, handler: InternalHookHandler) {
        handlers.getOrPut(eventKey) { mutableListOf() }.add(handler)
        Log.d(TAG, "Registered hook handler for: $eventKey")
    }

    /**
     * Unregister a hook handler.
     * Aligned with OpenClaw unregisterInternalHook.
     */
    fun unregister(eventKey: String, handler: InternalHookHandler) {
        handlers[eventKey]?.remove(handler)
    }

    /**
     * Clear all hooks.
     * Aligned with OpenClaw clearInternalHooks.
     */
    fun clear() {
        handlers.clear()
        Log.d(TAG, "All hooks cleared")
    }

    /**
     * Get registered event keys.
     * Aligned with OpenClaw getRegisteredEventKeys.
     */
    fun getRegisteredEventKeys(): Set<String> = handlers.keys.toSet()

    /**
     * Trigger a hook event.
     * Aligned with OpenClaw triggerInternalHook.
     *
     * Fires handlers for both the type key and the type:action key.
     */
    suspend fun trigger(event: InternalHookEvent) {
        val typeKey = event.type.name.lowercase()
        val actionKey = "$typeKey:${event.action}"

        val typeHandlers = handlers[typeKey] ?: emptyList()
        val actionHandlers = handlers[actionKey] ?: emptyList()

        for (handler in typeHandlers + actionHandlers) {
            try {
                handler(event)
            } catch (e: Exception) {
                Log.w(TAG, "Hook handler error for $actionKey: ${e.message}")
            }
        }
    }

    /**
     * Create a hook event (convenience factory).
     * Aligned with OpenClaw createInternalHookEvent.
     */
    fun createEvent(
        type: InternalHookEventType,
        action: String,
        sessionKey: String? = null,
        context: Map<String, Any?> = emptyMap()
    ): InternalHookEvent {
        return InternalHookEvent(
            type = type,
            action = action,
            sessionKey = sessionKey,
            context = context
        )
    }

    // ── Type guard convenience methods ──

    fun isAgentBootstrapEvent(event: InternalHookEvent): Boolean =
        event.type == InternalHookEventType.AGENT && event.action == "bootstrap"

    fun isGatewayStartupEvent(event: InternalHookEvent): Boolean =
        event.type == InternalHookEventType.GATEWAY && event.action == "startup"

    fun isMessageReceivedEvent(event: InternalHookEvent): Boolean =
        event.type == InternalHookEventType.MESSAGE && event.action == "received"

    fun isMessageSentEvent(event: InternalHookEvent): Boolean =
        event.type == InternalHookEventType.MESSAGE && event.action == "sent"
}
