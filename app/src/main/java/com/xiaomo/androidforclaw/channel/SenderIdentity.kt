package com.xiaomo.androidforclaw.channel

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/channels/sender-identity.ts (validateSenderIdentity)
 * - ../openclaw/src/channels/sender-label.ts
 *
 * AndroidForClaw adaptation: sender identity validation and label resolution.
 */

/**
 * Sender identity fields from inbound messages.
 * Aligned with OpenClaw MsgContext sender fields.
 */
data class SenderIdentity(
    val senderId: String? = null,
    val senderName: String? = null,
    val senderUsername: String? = null,
    val senderE164: String? = null,  // E.164 phone number
    val chatType: String? = null     // "direct" / "group" / "channel" / "thread"
)

/**
 * SenderIdentity validation and label resolution.
 * Aligned with OpenClaw sender-identity.ts and sender-label.ts.
 */
object SenderIdentityValidator {

    /** E.164 phone number pattern */
    private val E164_PATTERN = Regex("^\\+\\d{3,}$")

    /** Username must not contain @ or whitespace */
    private val USERNAME_INVALID_PATTERN = Regex("[@\\s]")

    /**
     * Validate sender identity fields.
     * Aligned with OpenClaw validateSenderIdentity.
     *
     * @return List of validation issues (empty = valid)
     */
    fun validate(identity: SenderIdentity): List<String> {
        val issues = mutableListOf<String>()

        // Non-direct chats must have at least one sender field
        if (identity.chatType != null && identity.chatType != "direct" && identity.chatType != "p2p") {
            val hasSender = !identity.senderId.isNullOrBlank() ||
                !identity.senderName.isNullOrBlank() ||
                !identity.senderUsername.isNullOrBlank() ||
                !identity.senderE164.isNullOrBlank()

            if (!hasSender) {
                issues.add("Non-direct chat must have at least one sender field (senderId/senderName/senderUsername/senderE164)")
            }
        }

        // E.164 validation
        if (!identity.senderE164.isNullOrBlank()) {
            if (!E164_PATTERN.matches(identity.senderE164)) {
                issues.add("SenderE164 must match E.164 format (e.g., +8613800138000), got: ${identity.senderE164}")
            }
        }

        // Username validation
        if (!identity.senderUsername.isNullOrBlank()) {
            if (USERNAME_INVALID_PATTERN.containsMatchIn(identity.senderUsername)) {
                issues.add("SenderUsername must not contain @ or whitespace, got: ${identity.senderUsername}")
            }
        }

        // SenderId must not be set-but-empty
        if (identity.senderId != null && identity.senderId.isBlank()) {
            issues.add("SenderId is set but empty")
        }

        return issues
    }

    /**
     * Build a display label for a sender.
     * Aligned with OpenClaw sender-label.ts.
     *
     * Priority: senderName > senderUsername > senderId > "Unknown"
     */
    fun buildLabel(identity: SenderIdentity): String {
        return when {
            !identity.senderName.isNullOrBlank() -> identity.senderName
            !identity.senderUsername.isNullOrBlank() -> "@${identity.senderUsername}"
            !identity.senderId.isNullOrBlank() -> identity.senderId
            else -> "Unknown"
        }
    }

    /**
     * Build a unique sender key for deduplication.
     */
    fun buildSenderKey(identity: SenderIdentity): String {
        return identity.senderId
            ?: identity.senderUsername
            ?: identity.senderE164
            ?: identity.senderName
            ?: "anonymous"
    }
}
