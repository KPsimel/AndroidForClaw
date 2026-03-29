package com.xiaomo.androidforclaw.secrets

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/secrets/runtime.ts
 *   (prepareSecretsRuntimeSnapshot, activateSecretsRuntimeSnapshot)
 * - ../openclaw/src/secrets/resolve.ts (resolveSecretRef)
 * - ../openclaw/src/secrets/target-registry.ts
 *
 * AndroidForClaw adaptation: runtime secrets management.
 * Manages secrets lifecycle (prepare → activate → query → clear).
 */

import com.xiaomo.androidforclaw.config.OpenClawConfig
import com.xiaomo.androidforclaw.logging.Log

/**
 * Secret reference — pointer to a secret value in external storage.
 * Aligned with OpenClaw SecretRef.
 */
data class SecretRef(
    val source: String,   // "env", "keystore", "file", "config"
    val key: String,      // the key/path to resolve
    val fallback: String? = null
)

/**
 * Secret resolver warning.
 * Aligned with OpenClaw SecretResolverWarning.
 */
data class SecretResolverWarning(
    val path: String,
    val message: String
)

/**
 * Runtime secrets snapshot.
 * Aligned with OpenClaw PreparedSecretsRuntimeSnapshot.
 */
data class SecretsRuntimeSnapshot(
    val sourceConfig: OpenClawConfig,
    val config: OpenClawConfig,
    val warnings: List<SecretResolverWarning>,
    val preparedAt: Long = System.currentTimeMillis()
)

/**
 * Secret target — a config path that expects a secret value.
 * Aligned with OpenClaw target-registry.ts.
 */
data class SecretTarget(
    val configPath: String,
    val description: String,
    val required: Boolean = false
)

/**
 * SecretsRuntime — Runtime secrets management.
 * Aligned with OpenClaw secrets/runtime.ts.
 */
object SecretsRuntime {

    private const val TAG = "SecretsRuntime"

    /** Active runtime snapshot (singleton) */
    @Volatile
    private var activeSnapshot: SecretsRuntimeSnapshot? = null

    /** Known secret targets in config */
    val SECRET_TARGETS = listOf(
        SecretTarget("channels.feishu.appSecret", "Feishu App Secret", true),
        SecretTarget("channels.feishu.encryptKey", "Feishu Encrypt Key"),
        SecretTarget("channels.discord.token", "Discord Bot Token", true),
        SecretTarget("channels.telegram.botToken", "Telegram Bot Token", true),
        SecretTarget("channels.slack.botToken", "Slack Bot Token", true),
        SecretTarget("channels.slack.appToken", "Slack App Token"),
        SecretTarget("channels.whatsapp.phoneNumber", "WhatsApp Phone Number"),
        SecretTarget("gateway.authToken", "Gateway Auth Token", true),
        SecretTarget("models.providers.*.apiKey", "Provider API Key", true)
    )

    /**
     * Prepare a secrets runtime snapshot.
     * Aligned with OpenClaw prepareSecretsRuntimeSnapshot.
     *
     * Deep-clones config and resolves any secret references.
     */
    fun prepare(config: OpenClawConfig): SecretsRuntimeSnapshot {
        val warnings = mutableListOf<SecretResolverWarning>()

        // On Android, secrets are stored in config directly (no external secret store).
        // Future: integrate with Android Keystore for encrypted storage.
        // For now, validate that required secrets are present.
        validateSecretTargets(config, warnings)

        return SecretsRuntimeSnapshot(
            sourceConfig = config,
            config = config,  // On Android, no transformation needed yet
            warnings = warnings
        )
    }

    /**
     * Activate a prepared snapshot.
     * Aligned with OpenClaw activateSecretsRuntimeSnapshot.
     */
    fun activate(snapshot: SecretsRuntimeSnapshot) {
        activeSnapshot = snapshot
        Log.i(TAG, "Secrets runtime snapshot activated (${snapshot.warnings.size} warnings)")
    }

    /**
     * Get the active snapshot.
     * Aligned with OpenClaw getActiveSecretsRuntimeSnapshot.
     */
    fun getActiveSnapshot(): SecretsRuntimeSnapshot? = activeSnapshot

    /**
     * Clear the active snapshot.
     * Aligned with OpenClaw clearSecretsRuntimeSnapshot.
     */
    fun clear() {
        activeSnapshot = null
        Log.d(TAG, "Secrets runtime snapshot cleared")
    }

    /**
     * Resolve a secret reference to its value.
     * Aligned with OpenClaw resolveSecretRef.
     */
    fun resolveSecretRef(ref: SecretRef): String? {
        return when (ref.source) {
            "env" -> System.getenv(ref.key) ?: ref.fallback
            "config" -> {
                // Read from active config
                val config = activeSnapshot?.config
                resolveConfigPath(config, ref.key) ?: ref.fallback
            }
            else -> ref.fallback
        }
    }

    /**
     * Validate that required secret targets have values.
     */
    private fun validateSecretTargets(config: OpenClawConfig, warnings: MutableList<SecretResolverWarning>) {
        for (target in SECRET_TARGETS) {
            if (!target.required) continue
            val value = resolveConfigPath(config, target.configPath)
            if (value.isNullOrBlank()) {
                warnings.add(SecretResolverWarning(
                    path = target.configPath,
                    message = "${target.description} is not configured"
                ))
            }
        }
    }

    /**
     * Simple config path resolver (dot-notation).
     * Resolves paths like "channels.feishu.appSecret" against OpenClawConfig.
     */
    private fun resolveConfigPath(config: OpenClawConfig?, path: String): String? {
        if (config == null) return null
        // Simple lookup for known paths
        return when (path) {
            "channels.feishu.appSecret" -> config.channels?.feishu?.appSecret
            "channels.feishu.encryptKey" -> config.channels?.feishu?.encryptKey
            "channels.discord.token" -> config.channels?.discord?.token
            "channels.telegram.botToken" -> config.channels?.telegram?.botToken
            "channels.slack.botToken" -> config.channels?.slack?.botToken
            "channels.slack.appToken" -> config.channels?.slack?.appToken
            "gateway.authToken" -> config.gateway.auth?.token
            else -> null
        }
    }
}
