package com.xiaomo.androidforclaw.security

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/security/safe-regex.ts
 *   (compileSafeRegex, compileSafeRegexDetailed, hasNestedRepetition, testRegexWithBoundedInput)
 *
 * AndroidForClaw adaptation: safe regex compilation with ReDoS protection.
 * Detects patterns with nested repetition that can cause catastrophic backtracking.
 */

import com.xiaomo.androidforclaw.logging.Log

/**
 * Rejection reasons for unsafe regex patterns.
 * Aligned with OpenClaw SafeRegexRejectReason.
 */
enum class SafeRegexRejectReason {
    EMPTY,
    UNSAFE_NESTED_REPETITION,
    INVALID_REGEX
}

/**
 * Result of safe regex compilation.
 * Aligned with OpenClaw SafeRegexCompileResult.
 */
data class SafeRegexCompileResult(
    val regex: Regex?,
    val source: String,
    val flags: String,
    val reason: SafeRegexRejectReason?
)

/**
 * SafeRegex — Compile user-supplied regex patterns safely.
 * Aligned with OpenClaw safe-regex.ts.
 */
object SafeRegex {

    private const val TAG = "SafeRegex"

    /** Max cached regex patterns */
    const val CACHE_MAX = 256

    /** Max input window for bounded testing */
    const val TEST_WINDOW = 2048

    /** LRU cache for compiled regexes */
    private val cache = LinkedHashMap<String, SafeRegexCompileResult>(
        CACHE_MAX, 0.75f, true
    )

    /**
     * Check if a regex source has nested repetition (ReDoS-prone).
     * Aligned with OpenClaw hasNestedRepetition.
     *
     * Detects patterns like `(a+)+`, `(a*)*`, `(a{1,}){2,}` which can
     * cause exponential backtracking.
     */
    fun hasNestedRepetition(source: String): Boolean {
        // Simple heuristic: look for quantifier after group that contains quantifier
        val quantifiers = setOf('+', '*', '?')
        var depth = 0
        var hasQuantifierInGroup = false
        var i = 0

        while (i < source.length) {
            val c = source[i]

            when {
                c == '\\' -> i++ // skip escaped char
                c == '(' -> {
                    depth++
                    hasQuantifierInGroup = false
                }
                c == ')' -> {
                    depth--
                    // Check if next char is a quantifier
                    if (hasQuantifierInGroup && i + 1 < source.length) {
                        val next = source[i + 1]
                        if (next in quantifiers || next == '{') {
                            return true
                        }
                    }
                }
                c in quantifiers && depth > 0 -> {
                    hasQuantifierInGroup = true
                }
                c == '{' && depth > 0 -> {
                    // Check for {n,m} quantifier
                    val closeBrace = source.indexOf('}', i)
                    if (closeBrace > i && source.substring(i, closeBrace + 1).matches(Regex("\\{\\d+,\\d*}"))) {
                        hasQuantifierInGroup = true
                    }
                }
            }
            i++
        }

        return false
    }

    /**
     * Compile a regex safely with detailed result.
     * Aligned with OpenClaw compileSafeRegexDetailed.
     */
    fun compileDetailed(source: String, flags: String = ""): SafeRegexCompileResult {
        val cacheKey = "$source/$flags"

        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }

        val result = when {
            source.isBlank() -> SafeRegexCompileResult(null, source, flags, SafeRegexRejectReason.EMPTY)
            hasNestedRepetition(source) -> {
                Log.w(TAG, "Rejected unsafe regex (nested repetition): $source")
                SafeRegexCompileResult(null, source, flags, SafeRegexRejectReason.UNSAFE_NESTED_REPETITION)
            }
            else -> {
                try {
                    val options = mutableSetOf<RegexOption>()
                    if ('i' in flags) options.add(RegexOption.IGNORE_CASE)
                    if ('m' in flags) options.add(RegexOption.MULTILINE)
                    if ('s' in flags) options.add(RegexOption.DOT_MATCHES_ALL)
                    val regex = Regex(source, options)
                    SafeRegexCompileResult(regex, source, flags, null)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid regex: $source — ${e.message}")
                    SafeRegexCompileResult(null, source, flags, SafeRegexRejectReason.INVALID_REGEX)
                }
            }
        }

        synchronized(cache) {
            if (cache.size >= CACHE_MAX) {
                val eldest = cache.keys.first()
                cache.remove(eldest)
            }
            cache[cacheKey] = result
        }

        return result
    }

    /**
     * Compile a regex safely. Returns null if unsafe or invalid.
     * Aligned with OpenClaw compileSafeRegex.
     */
    fun compile(source: String, flags: String = ""): Regex? {
        return compileDetailed(source, flags).regex
    }

    /**
     * Test regex against bounded input to prevent long execution times.
     * Aligned with OpenClaw testRegexWithBoundedInput.
     */
    fun testWithBoundedInput(
        regex: Regex,
        input: String,
        maxWindow: Int = TEST_WINDOW
    ): Boolean {
        if (input.length <= maxWindow * 2) {
            return regex.containsMatchIn(input)
        }

        // Test first and last windows
        val head = input.substring(0, maxWindow)
        val tail = input.substring(input.length - maxWindow)

        return regex.containsMatchIn(head) || regex.containsMatchIn(tail)
    }
}
