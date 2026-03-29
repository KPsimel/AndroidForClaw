package com.xiaomo.androidforclaw.agent.memory

/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/memory/mmr.ts (MMRConfig, DEFAULT_MMR_CONFIG, applyMMR)
 * - ../openclaw/src/memory/hybrid.ts (mergeHybridResults — MMR integration)
 *
 * AndroidForClaw adaptation: Maximal Marginal Relevance re-ranking.
 * Improves diversity in search results by penalizing redundancy.
 */

/**
 * MMR configuration.
 * Aligned with OpenClaw MMRConfig.
 */
data class MMRConfig(
    /** Lambda: 1.0 = pure relevance, 0.0 = pure diversity */
    val lambda: Float = 0.7f,
    /** Whether MMR re-ranking is enabled */
    val enabled: Boolean = true
)

/** Default MMR configuration. Aligned with OpenClaw DEFAULT_MMR_CONFIG. */
val DEFAULT_MMR_CONFIG = MMRConfig(lambda = 0.7f, enabled = true)

/**
 * Temporal decay configuration for memory search results.
 * Aligned with OpenClaw TemporalDecayConfig.
 */
data class TemporalDecayConfig(
    /** Whether temporal decay is enabled */
    val enabled: Boolean = false,
    /** Half-life in days — score halves every N days */
    val halfLifeDays: Float = 7f
)

val DEFAULT_TEMPORAL_DECAY_CONFIG = TemporalDecayConfig()

/**
 * MMRReranker — Maximal Marginal Relevance re-ranking for search results.
 * Aligned with OpenClaw mmr.ts.
 *
 * MMR balances relevance and diversity:
 * MMR = λ * sim(d, q) - (1-λ) * max(sim(d, d_j)) for d_j in selected
 *
 * Without embeddings, we approximate diversity using snippet text overlap.
 */
object MMRReranker {

    /**
     * Apply MMR re-ranking to search results.
     * Aligned with OpenClaw applyMMR.
     *
     * @param results Input results sorted by relevance score
     * @param config MMR configuration
     * @param maxResults Maximum results to return
     * @return Re-ranked results
     */
    fun <T> apply(
        results: List<T>,
        config: MMRConfig = DEFAULT_MMR_CONFIG,
        maxResults: Int = results.size,
        scoreSelector: (T) -> Float,
        snippetSelector: (T) -> String,
        copyWithScore: (T, Float) -> T
    ): List<T> {
        if (!config.enabled || results.size <= 1) return results.take(maxResults)

        val selected = mutableListOf<T>()
        val remaining = results.toMutableList()

        while (selected.size < maxResults && remaining.isNotEmpty()) {
            var bestIdx = -1
            var bestMmrScore = Float.NEGATIVE_INFINITY

            for (i in remaining.indices) {
                val candidate = remaining[i]
                val relevanceScore = scoreSelector(candidate)

                // Compute max similarity to already-selected results
                val maxSimilarity = if (selected.isEmpty()) {
                    0f
                } else {
                    selected.maxOf { sel ->
                        textOverlap(snippetSelector(candidate), snippetSelector(sel))
                    }
                }

                // MMR score = λ * relevance - (1-λ) * maxSimilarity
                val mmrScore = config.lambda * relevanceScore - (1f - config.lambda) * maxSimilarity

                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore
                    bestIdx = i
                }
            }

            if (bestIdx >= 0) {
                val best = remaining.removeAt(bestIdx)
                selected.add(copyWithScore(best, bestMmrScore))
            } else {
                break
            }
        }

        return selected
    }

    /**
     * Apply temporal decay to a score.
     * Aligned with OpenClaw temporal decay in mergeHybridResults.
     *
     * @param score Original score
     * @param ageMs Age of the result in milliseconds
     * @param config Temporal decay configuration
     * @return Decayed score
     */
    fun applyTemporalDecay(
        score: Float,
        ageMs: Long,
        config: TemporalDecayConfig = DEFAULT_TEMPORAL_DECAY_CONFIG
    ): Float {
        if (!config.enabled || ageMs <= 0) return score
        val ageDays = ageMs / (24 * 3600 * 1000f)
        val decayFactor = Math.pow(0.5, (ageDays / config.halfLifeDays).toDouble()).toFloat()
        return score * decayFactor
    }

    /**
     * Compute text overlap between two snippets (0.0 to 1.0).
     * Simple Jaccard similarity on word-level trigrams.
     */
    private fun textOverlap(a: String, b: String): Float {
        val tokensA = extractNgrams(a, 3)
        val tokensB = extractNgrams(b, 3)
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0f
        val intersection = tokensA.intersect(tokensB).size.toFloat()
        val union = tokensA.union(tokensB).size.toFloat()
        return if (union > 0) intersection / union else 0f
    }

    /** Extract word-level n-grams */
    private fun extractNgrams(text: String, n: Int): Set<String> {
        val words = text.lowercase().split(Regex("\\W+")).filter { it.isNotEmpty() }
        if (words.size < n) return words.toSet()
        return words.windowed(n).map { it.joinToString(" ") }.toSet()
    }
}
