package com.xiaomo.androidforclaw.providers

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/agents/auth-profiles/types.ts
 *   (AuthProfileCredential, AuthProfileStore, ProfileUsageStats, AuthProfileFailureReason)
 * - ../openclaw/src/agents/auth-profiles/profiles.ts
 *   (upsertAuthProfile, listProfilesForProvider, markAuthProfileGood, setAuthProfileOrder)
 * - ../openclaw/src/agents/auth-profiles/usage.ts
 *   (recordProfileFailure, recordProfileSuccess, isProfileCoolingDown)
 * - ../openclaw/src/agents/auth-profiles/order.ts
 *   (resolveProfileOrder, advanceRoundRobinOrder)
 *
 * AndroidForClaw adaptation: multi-provider credential profile management.
 * Supports API key rotation, cooldown on failure, round-robin ordering.
 */

import com.xiaomo.androidforclaw.logging.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Credential types.
 * Aligned with OpenClaw AuthProfileCredential.
 */
sealed class AuthProfileCredential {
    abstract val provider: String
    abstract val email: String?

    data class ApiKey(
        override val provider: String,
        val key: String,
        override val email: String? = null,
        val metadata: Map<String, String>? = null
    ) : AuthProfileCredential()

    data class Token(
        override val provider: String,
        val token: String,
        val expires: Long? = null,
        override val email: String? = null
    ) : AuthProfileCredential()

    data class OAuth(
        override val provider: String,
        val clientId: String?,
        val accessToken: String?,
        val refreshToken: String?,
        val expiresAt: Long? = null,
        override val email: String? = null
    ) : AuthProfileCredential()
}

/**
 * Profile failure reasons.
 * Aligned with OpenClaw AuthProfileFailureReason.
 */
enum class AuthProfileFailureReason {
    AUTH,
    AUTH_PERMANENT,
    FORMAT,
    OVERLOADED,
    RATE_LIMIT,
    BILLING,
    TIMEOUT,
    MODEL_NOT_FOUND,
    SESSION_EXPIRED,
    UNKNOWN
}

/**
 * Per-profile usage statistics.
 * Aligned with OpenClaw ProfileUsageStats.
 */
data class ProfileUsageStats(
    var lastUsed: Long? = null,
    var cooldownUntil: Long? = null,
    var disabledUntil: Long? = null,
    var disabledReason: AuthProfileFailureReason? = null,
    var errorCount: Int = 0,
    var failureCounts: MutableMap<String, Int> = mutableMapOf(),
    var lastFailureAt: Long? = null
)

/**
 * Auth profile store (serialized to JSON).
 * Aligned with OpenClaw AuthProfileStore.
 */
data class AuthProfileStore(
    val version: Int = 1,
    val profiles: MutableMap<String, AuthProfileCredential> = mutableMapOf(),
    val order: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val lastGood: MutableMap<String, String> = mutableMapOf(),
    val usageStats: MutableMap<String, ProfileUsageStats> = mutableMapOf()
)

/**
 * AuthProfiles — Multi-provider credential profile management.
 * Aligned with OpenClaw auth-profiles.
 */
object AuthProfiles {

    private const val TAG = "AuthProfiles"
    private const val STORE_FILE = "auth-profiles.json"

    /** Default cooldown after rate limit (60 seconds) */
    const val DEFAULT_RATE_LIMIT_COOLDOWN_MS = 60_000L

    /** Default cooldown after auth failure (5 minutes) */
    const val DEFAULT_AUTH_COOLDOWN_MS = 5 * 60_000L

    /** Max consecutive errors before disabling */
    const val MAX_CONSECUTIVE_ERRORS = 5

    /** In-memory store */
    private var store = AuthProfileStore()

    /** Runtime usage stats (not persisted) */
    private val runtimeStats = ConcurrentHashMap<String, ProfileUsageStats>()

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Load store from disk.
     */
    fun load(workspaceDir: File) {
        val file = File(workspaceDir, STORE_FILE)
        if (file.exists()) {
            try {
                store = gson.fromJson(file.readText(), AuthProfileStore::class.java) ?: AuthProfileStore()
                Log.d(TAG, "Loaded ${store.profiles.size} auth profiles")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load auth profiles: ${e.message}")
                store = AuthProfileStore()
            }
        }
    }

    /**
     * Save store to disk.
     */
    fun save(workspaceDir: File) {
        val file = File(workspaceDir, STORE_FILE)
        try {
            file.writeText(gson.toJson(store))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save auth profiles: ${e.message}")
        }
    }

    /**
     * Upsert (create or update) an auth profile.
     * Aligned with OpenClaw upsertAuthProfile.
     */
    fun upsert(profileId: String, credential: AuthProfileCredential) {
        store.profiles[profileId] = credential
        Log.d(TAG, "Upserted profile: $profileId (provider=${credential.provider})")
    }

    /**
     * List profile IDs for a given provider.
     * Aligned with OpenClaw listProfilesForProvider.
     */
    fun listForProvider(provider: String): List<String> {
        return store.profiles.entries
            .filter { it.value.provider.equals(provider, ignoreCase = true) }
            .map { it.key }
    }

    /**
     * Get a profile credential by ID.
     */
    fun get(profileId: String): AuthProfileCredential? = store.profiles[profileId]

    /**
     * Remove a profile.
     */
    fun remove(profileId: String) {
        store.profiles.remove(profileId)
        store.usageStats.remove(profileId)
        runtimeStats.remove(profileId)
    }

    /**
     * Mark a profile as the last known good for a provider.
     * Aligned with OpenClaw markAuthProfileGood.
     */
    fun markGood(provider: String, profileId: String) {
        store.lastGood[provider] = profileId
        val stats = getOrCreateStats(profileId)
        stats.lastUsed = System.currentTimeMillis()
        stats.errorCount = 0
    }

    /**
     * Record a profile failure and apply cooldown.
     * Aligned with OpenClaw recordProfileFailure.
     */
    fun recordFailure(profileId: String, reason: AuthProfileFailureReason) {
        val stats = getOrCreateStats(profileId)
        stats.errorCount++
        stats.lastFailureAt = System.currentTimeMillis()
        stats.failureCounts[reason.name] = (stats.failureCounts[reason.name] ?: 0) + 1

        // Apply cooldown based on failure type
        val cooldownMs = when (reason) {
            AuthProfileFailureReason.RATE_LIMIT -> DEFAULT_RATE_LIMIT_COOLDOWN_MS
            AuthProfileFailureReason.AUTH, AuthProfileFailureReason.BILLING -> DEFAULT_AUTH_COOLDOWN_MS
            AuthProfileFailureReason.AUTH_PERMANENT -> Long.MAX_VALUE  // permanently disabled
            else -> DEFAULT_RATE_LIMIT_COOLDOWN_MS
        }

        if (cooldownMs < Long.MAX_VALUE) {
            stats.cooldownUntil = System.currentTimeMillis() + cooldownMs
        } else {
            stats.disabledUntil = Long.MAX_VALUE
            stats.disabledReason = reason
        }

        // Disable after too many consecutive errors
        if (stats.errorCount >= MAX_CONSECUTIVE_ERRORS) {
            stats.disabledUntil = System.currentTimeMillis() + DEFAULT_AUTH_COOLDOWN_MS * 2
            stats.disabledReason = reason
            Log.w(TAG, "Profile $profileId disabled after ${stats.errorCount} consecutive errors")
        }

        Log.d(TAG, "Profile $profileId failure recorded: $reason (errors=${stats.errorCount})")
    }

    /**
     * Check if a profile is in cooldown.
     * Aligned with OpenClaw isProfileCoolingDown.
     */
    fun isCoolingDown(profileId: String): Boolean {
        val stats = runtimeStats[profileId] ?: return false
        val now = System.currentTimeMillis()

        if (stats.disabledUntil != null && now < stats.disabledUntil!!) return true
        if (stats.cooldownUntil != null && now < stats.cooldownUntil!!) return true

        // Cooldown expired, clear it
        if (stats.cooldownUntil != null && now >= stats.cooldownUntil!!) {
            stats.cooldownUntil = null
        }
        return false
    }

    /**
     * Resolve profile order for a provider (round-robin).
     * Aligned with OpenClaw resolveProfileOrder.
     */
    fun resolveOrder(provider: String): List<String> {
        val customOrder = store.order[provider]
        if (!customOrder.isNullOrEmpty()) {
            return customOrder.filter { !isCoolingDown(it) }
        }

        // Default: lastGood first, then all others
        val all = listForProvider(provider)
        val lastGood = store.lastGood[provider]
        val available = all.filter { !isCoolingDown(it) }

        return if (lastGood != null && lastGood in available) {
            listOf(lastGood) + available.filter { it != lastGood }
        } else {
            available
        }
    }

    /**
     * Set custom profile rotation order.
     * Aligned with OpenClaw setAuthProfileOrder.
     */
    fun setOrder(provider: String, order: List<String>?) {
        if (order.isNullOrEmpty()) {
            store.order.remove(provider)
        } else {
            store.order[provider] = order.toMutableList()
        }
    }

    /**
     * Get all profiles count.
     */
    fun profileCount(): Int = store.profiles.size

    private fun getOrCreateStats(profileId: String): ProfileUsageStats {
        return runtimeStats.getOrPut(profileId) { ProfileUsageStats() }
    }
}
