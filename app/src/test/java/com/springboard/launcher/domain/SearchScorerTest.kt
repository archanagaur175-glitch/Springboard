package com.springboard.launcher.domain

import com.springboard.launcher.data.apps.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScorerTest {

    @Test
    fun `empty query scores zero`() {
        assertEquals(0, SearchScorer.score("Google Maps", ""))
        assertEquals(0, SearchScorer.score("Google Maps", "   "))
    }

    @Test
    fun `prefix match scores highest`() {
        val label = "Google Maps"
        val score = SearchScorer.score(label, "go")
        assertTrue("expected a strong prefix score, got $score", score >= 90)
    }

    @Test
    fun `word boundary match is strong`() {
        val score = SearchScorer.score("Google Hangouts", "hangouts")
        assertEquals(89, score)
    }

    @Test
    fun `substring match is weaker than prefix`() {
        val prefix = SearchScorer.score("Camera", "cam")
        val substring = SearchScorer.score("Camera", "era")
        assertTrue(substring > 0)
        assertTrue(prefix > substring)
        assertEquals(67, substring)
    }

    @Test
    fun `package name can rescue a label miss`() {
        val labelScore = SearchScorer.score("Maps", "com.google.maps")
        val bothScore = SearchScorer.score("Maps", "com.google.maps", "google")
        assertTrue(bothScore > labelScore)
        assertTrue(bothScore > 0)
    }

    @Test
    fun `no match scores minus one`() {
        assertEquals(-1, SearchScorer.score("Chrome", "zzz"))
    }

    @Test
    fun `rank filters and orders by relevance`() {
        val apps = listOf(
            InstalledApp("com.zzz.not", "Not A Thing", 1),
            InstalledApp("com.spotify.music", "Spotify", 1),
            InstalledApp("com.spotify.ads", "Spotify Ads", 1),
        )
        val ranked = SearchScorer.rank(apps, "spot")
        assertEquals(listOf("Spotify", "Spotify Ads"), ranked.map { it.label })
    }

    @Test
    fun `blank query returns apps untouched`() {
        val apps = listOf(InstalledApp("com.a", "A", 1))
        assertEquals(apps, SearchScorer.rank(apps, "  "))
    }
}