package com.xiaomo.androidforclaw.security

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/security/dangerous-config-flags.ts
 * - ../openclaw/src/security/dangerous-tools.ts
 *
 * AndroidForClaw adaptation: detect dangerous configuration flags and tool settings.
 */

import com.xiaomo.androidforclaw.config.OpenClawConfig

/**
 * DangerousConfigFlags — Detect dangerous configuration.
 * Aligned with OpenClaw dangerous-config-flags.ts.
 */
object DangerousConfigFlags {

    /**
     * Check configuration for dangerous settings.
     * Returns list of warnings.
     */
    fun check(config: OpenClawConfig): List<String> {
        val warnings = mutableListOf<String>()

        // Check for overly permissive channel policies
        val channels = config.channels
        channels?.feishu?.let { feishu ->
            if (feishu.enabled && feishu.dmPolicy == "open" && feishu.groupPolicy == "open") {
                warnings.add("Feishu: both DM and group policies are 'open' — no access control")
            }
        }

        channels?.discord?.let { discord ->
            if (discord.enabled && discord.dm?.policy == "open" && discord.groupPolicy == "open") {
                warnings.add("Discord: both DM and group policies are 'open' — no access control")
            }
        }

        // Check for subagent depth too high
        config.agents?.defaults?.subagents?.let { sub ->
            if (sub.maxSpawnDepth > 5) {
                warnings.add("agents.subagents.maxSpawnDepth=${sub.maxSpawnDepth} — deeply nested subagents may be hard to control")
            }
            if (sub.maxConcurrent > 20) {
                warnings.add("agents.subagents.maxConcurrent=${sub.maxConcurrent} — high concurrency may exhaust resources")
            }
        }

        return warnings
    }
}
