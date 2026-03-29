package com.xiaomo.androidforclaw.security

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/security/external-content.ts
 *
 * AndroidForClaw adaptation: sanitize external content before injection into prompts.
 * Prevents prompt injection attacks from user-provided URLs, files, or API responses.
 */

/**
 * ExternalContentSanitizer — Sanitize external content for safe prompt inclusion.
 * Aligned with OpenClaw external-content.ts.
 */
object ExternalContentSanitizer {

    /** Max external content length (characters) */
    const val MAX_EXTERNAL_CONTENT_CHARS = 100_000

    /** Patterns that look like prompt injection attempts */
    private val INJECTION_PATTERNS = listOf(
        Regex("(?i)ignore\\s+(all\\s+)?previous\\s+instructions"),
        Regex("(?i)you\\s+are\\s+now\\s+(a|an)\\s+"),
        Regex("(?i)system\\s*:\\s*you\\s+are"),
        Regex("(?i)\\[INST\\]|\\[/INST\\]|<<SYS>>|<</SYS>>"),
        Regex("(?i)<\\|im_start\\|>|<\\|im_end\\|>"),
        Regex("(?i)Human:|Assistant:|System:")
    )

    /**
     * Sanitize external content for safe inclusion in prompts.
     *
     * @param content The raw external content
     * @param source Description of the content source (for logging)
     * @return Sanitized content, or null if too suspicious
     */
    fun sanitize(content: String, source: String = "external"): Pair<String, List<String>> {
        val warnings = mutableListOf<String>()
        var sanitized = content

        // Truncate if too long
        if (sanitized.length > MAX_EXTERNAL_CONTENT_CHARS) {
            sanitized = sanitized.take(MAX_EXTERNAL_CONTENT_CHARS) + "\n...(truncated)..."
            warnings.add("Content from $source truncated to $MAX_EXTERNAL_CONTENT_CHARS chars")
        }

        // Check for injection patterns
        for (pattern in INJECTION_PATTERNS) {
            if (pattern.containsMatchIn(sanitized)) {
                warnings.add("Suspicious pattern detected in $source: ${pattern.pattern}")
            }
        }

        return Pair(sanitized, warnings)
    }

    /**
     * Wrap external content with clear delimiters.
     * This helps the LLM distinguish between instructions and external data.
     */
    fun wrapWithDelimiters(content: String, source: String): String {
        return """
<external-content source="$source">
$content
</external-content>
        """.trimIndent()
    }
}
