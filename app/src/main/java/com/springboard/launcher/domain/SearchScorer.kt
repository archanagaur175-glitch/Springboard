package com.springboard.launcher.domain

/**
 * Spotlight-style fuzzy search scorer. Returns a relevance score for a label against a
 * query, or -1 when there is no match. Higher scores rank earlier. Matching is done on
 * both the display label and the package name (an app whose package matches is a fine hit).
 */
object SearchScorer {

    fun score(label: String, query: String): Int {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return 0
        return maxOf(scoreAgainst(label, needle), scoreAgainst(label, needle))
    }

    /** Scores label + package; takes the best of the two. */
    fun score(label: String, packageName: String, query: String): Int {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return 0
        return maxOf(scoreAgainst(label, needle), scoreAgainst(packageName, needle))
    }

    private fun scoreAgainst(haystack: String, needle: String): Int {
        val hay = haystack.lowercase()
        if (needle !in hay) return -1

        // Exact prefix match is the strongest signal.
        if (hay.startsWith(needle)) return 100 - (hay.length - needle.length)

        // Word-boundary match ("gh" matching "Google Hangouts") is also strong.
        val words = hay.split(' ', '-', '_', '.')
        val wordIndex = words.indexOfFirst { it == needle }
        if (wordIndex >= 0) return 90 - wordIndex

        // Substring match, lightly penalised by how deep it is in the name.
        return 70 - hay.indexOf(needle)
    }

    fun rank(
        apps: List<com.springboard.launcher.data.apps.InstalledApp>,
        query: String,
    ): List<com.springboard.launcher.data.apps.InstalledApp> {
        val needle = query.trim()
        if (needle.isEmpty()) return apps
        return apps
            .map { it to score(it.label, it.packageName, needle) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<com.springboard.launcher.data.apps.InstalledApp, Int>> { it.second }
                .thenBy { it.first.label.lowercase() })
            .map { it.first }
    }
}