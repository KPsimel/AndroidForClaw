package com.xiaomo.androidforclaw.cron

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/cron/delivery.ts
 *   (resolveCronDeliveryPlan, resolveFailureDestination, sendFailureNotificationAnnounce)
 *
 * AndroidForClaw adaptation: cron job output delivery to channels.
 * Routes cron results back to the originating channel/chat.
 */

import com.xiaomo.androidforclaw.logging.Log

/**
 * Resolved delivery plan — where to send cron job output.
 * Aligned with OpenClaw CronDeliveryPlan.
 */
data class CronDeliveryPlan(
    val mode: DeliveryMode,
    val channel: String? = null,
    val to: String? = null,
    val source: String = "delivery",  // "delivery" | "payload"
    val requested: Boolean = false
)

/** Failure notification timeout */
const val FAILURE_NOTIFICATION_TIMEOUT_MS = 30_000L

/**
 * CronDeliveryResolver — Cron job output delivery resolution.
 * Aligned with OpenClaw cron/delivery.ts.
 */
object CronDeliveryResolver {

    private const val TAG = "CronDeliveryResolver"

    /**
     * Resolve delivery plan for a cron job.
     * Aligned with OpenClaw resolveCronDeliveryPlan.
     */
    fun resolveDeliveryPlan(job: CronJob): CronDeliveryPlan {
        // Check explicit delivery config on CronJob
        val delivery = job.delivery
        if (delivery != null) {
            return CronDeliveryPlan(
                mode = delivery.mode,
                channel = delivery.channel,
                to = delivery.to,
                source = "delivery",
                requested = true
            )
        }

        // Legacy: check payload for delivery hints
        val payload = job.payload
        if (payload is CronPayload.AgentTurn) {
            if (payload.deliver == true && !payload.channel.isNullOrBlank()) {
                return CronDeliveryPlan(
                    mode = DeliveryMode.ANNOUNCE,
                    channel = payload.channel,
                    to = payload.to,
                    source = "payload",
                    requested = true
                )
            }
            if (payload.deliver == false) {
                return CronDeliveryPlan(mode = DeliveryMode.NONE, source = "payload")
            }
        }

        return CronDeliveryPlan(mode = DeliveryMode.NONE)
    }

    /**
     * Format a cron result message for delivery.
     */
    fun formatResultMessage(jobId: String, jobDescription: String?, result: String): String {
        val desc = jobDescription ?: jobId
        return "[Cron: $desc]\n$result"
    }

    /**
     * Format a failure notification message.
     */
    fun formatFailureMessage(
        jobId: String,
        jobDescription: String?,
        error: String,
        consecutiveErrors: Int
    ): String {
        val desc = jobDescription ?: jobId
        return "[Cron Failure: $desc]\nError: $error\nConsecutive failures: $consecutiveErrors"
    }
}
