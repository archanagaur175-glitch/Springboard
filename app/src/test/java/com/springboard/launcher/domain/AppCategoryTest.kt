package com.springboard.launcher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryTest {

    @Test
    fun `known keywords map to the right category`() {
        assertEquals("Entertainment", AppCategory.categoryFor("Spotify", "com.spotify.music"))
        assertEquals("Travel", AppCategory.categoryFor("Google Maps", "com.google.android.apps.maps"))
        assertEquals("Productivity", AppCategory.categoryFor("Calculator", "com.example.calculator"))
        assertEquals("Social", AppCategory.categoryFor("Instagram", "com.instagram.android"))
        assertEquals("Games", AppCategory.categoryFor("Angry Birds", "com.rovio.angry"))
    }

    @Test
    fun `unknown apps fall into Other`() {
        assertEquals("Other", AppCategory.categoryFor("Random Widget", "com.unknown.random"))
    }

    @Test
    fun `category check is case and package aware`() {
        val viaPackage = AppCategory.categoryFor("Launcher", "com.android.settings")
        assertEquals("System", viaPackage)
    }

    @Test
    fun `sorted categories preserve map order and append Other`() {
        val labels = listOf(
            "Just An App" to "com.x.app",
            "YouTube" to "com.google.android.youtube",
            "WhatsApp" to "com.whatsapp",
        )
        val sorted = AppCategory.sortedCategoryNames(labels)
        assertEquals(listOf("Entertainment", "Social", "Other"), sorted)
    }

    @Test
    fun `empty set yields no categories`() {
        assertEquals(emptyList<String>(), AppCategory.sortedCategoryNames(emptyList()))
    }
}