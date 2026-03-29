package com.xiaomo.androidforclaw.agent.memory

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/memory/query-expansion.ts (extractKeywords, expandQuery)
 * - ../openclaw/src/memory/hybrid.ts (buildFtsQuery)
 *
 * AndroidForClaw adaptation: keyword extraction and query expansion for memory search.
 */

/**
 * QueryExpansion — Extract keywords and build FTS5 queries.
 * Aligned with OpenClaw query-expansion.ts.
 */
object QueryExpansion {

    /** Common stop words to exclude from keyword extraction */
    private val STOP_WORDS = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "shall", "can", "need", "dare", "ought",
        "used", "to", "of", "in", "for", "on", "with", "at", "by", "from",
        "as", "into", "through", "during", "before", "after", "above", "below",
        "between", "out", "off", "over", "under", "again", "further", "then",
        "once", "here", "there", "when", "where", "why", "how", "all", "both",
        "each", "few", "more", "most", "other", "some", "such", "no", "nor",
        "not", "only", "own", "same", "so", "than", "too", "very", "just",
        "don", "now", "and", "but", "or", "if", "that", "this", "it", "its",
        "what", "which", "who", "whom", "these", "those", "i", "me", "my",
        "we", "our", "you", "your", "he", "him", "his", "she", "her", "they",
        "them", "their", "about", "up"
    )

    /** Unicode word token pattern (aligned with OpenClaw /[\p{L}\p{N}_]+/gu) */
    private val TOKEN_PATTERN = Regex("[\\p{L}\\p{N}_]+")

    /**
     * Extract keywords from a query string.
     * Aligned with OpenClaw extractKeywords.
     */
    fun extractKeywords(query: String): List<String> {
        return TOKEN_PATTERN.findAll(query.lowercase())
            .map { it.value }
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .distinct()
            .toList()
    }

    /**
     * Build an FTS5-compatible query string.
     * Aligned with OpenClaw buildFtsQuery.
     *
     * Tokenizes input and joins with "AND" for conjunctive search.
     * Returns null if no valid tokens.
     */
    fun buildFtsQuery(raw: String): String? {
        val keywords = extractKeywords(raw)
        if (keywords.isEmpty()) return null
        return keywords.joinToString(" AND ")
    }

    /**
     * Expand a query with synonym/related terms for broader recall.
     * Simple expansion: extract keywords + generate prefix variants.
     */
    fun expandQuery(query: String): List<String> {
        val keywords = extractKeywords(query)
        val expanded = mutableListOf(query)  // original query first

        // Add individual keywords as separate queries
        if (keywords.size > 1) {
            for (keyword in keywords) {
                if (keyword.length >= 4) {
                    expanded.add(keyword)
                }
            }
        }

        return expanded.distinct()
    }
}
