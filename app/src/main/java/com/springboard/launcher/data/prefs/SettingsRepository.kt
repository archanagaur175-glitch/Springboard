package com.springboard.launcher.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Process-wide cached prefs so broadcast receivers (screen-on) can read the lock-facade
 * flag synchronously without blocking on DataStore.
 */
object AppPrefs {
    @Volatile var onboardingComplete: Boolean = false
    @Volatile var wallpaperIndex: Int = 0
    @Volatile var lockFacadeEnabled: Boolean = false
    @Volatile var tier2CcEnabled: Boolean = false
}

val Context.springboardDataStore: DataStore<Preferences> by preferencesDataStore(name = "springboard_prefs")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val WALLPAPER_INDEX = intPreferencesKey("wallpaper_index")
        val LOCK_FACADE_ENABLED = booleanPreferencesKey("lock_facade_enabled")
        val TIER2_CC_ENABLED = booleanPreferencesKey("tier2_cc_enabled")
        val NC_RATIONALE_SEEN = booleanPreferencesKey("nc_rationale_seen")
        val OVERLAY_RATIONALE_SEEN = booleanPreferencesKey("overlay_rationale_seen")
        val BRIGHTNESS_RATIONALE_SEEN = booleanPreferencesKey("brightness_rationale_seen")
        val RECENT_PACKAGES = stringPreferencesKey("recent_packages")
    }

    /** Reads all prefs into the synchronous [AppPrefs] cache. */
    suspend fun hydrate() {
        val p = dataStore.data.first()
        AppPrefs.onboardingComplete = p[Keys.ONBOARDING_COMPLETE] ?: false
        AppPrefs.wallpaperIndex = p[Keys.WALLPAPER_INDEX] ?: 0
        AppPrefs.lockFacadeEnabled = p[Keys.LOCK_FACADE_ENABLED] ?: false
        AppPrefs.tier2CcEnabled = p[Keys.TIER2_CC_ENABLED] ?: false
    }

    val onboardingCompleteFlow: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val wallpaperFlow: Flow<Int> = dataStore.data.map { it[Keys.WALLPAPER_INDEX] ?: 0 }
    val lockFacadeFlow: Flow<Boolean> = dataStore.data.map { it[Keys.LOCK_FACADE_ENABLED] ?: false }
    val tier2CcFlow: Flow<Boolean> = dataStore.data.map { it[Keys.TIER2_CC_ENABLED] ?: false }

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
        AppPrefs.onboardingComplete = value
    }

    suspend fun setWallpaperIndex(value: Int) {
        dataStore.edit { it[Keys.WALLPAPER_INDEX] = value }
        AppPrefs.wallpaperIndex = value
    }

    suspend fun setLockFacadeEnabled(value: Boolean) {
        dataStore.edit { it[Keys.LOCK_FACADE_ENABLED] = value }
        AppPrefs.lockFacadeEnabled = value
    }

    suspend fun setTier2CcEnabled(value: Boolean) {
        dataStore.edit { it[Keys.TIER2_CC_ENABLED] = value }
        AppPrefs.tier2CcEnabled = value
    }

    suspend fun setCurrentPage(value: Int) {
        dataStore.edit { it[Keys.CURRENT_PAGE] = value }
    }

    suspend fun setNcRationaleSeen() {
        dataStore.edit { it[Keys.NC_RATIONALE_SEEN] = true }
    }

    suspend fun setOverlayRationaleSeen() {
        dataStore.edit { it[Keys.OVERLAY_RATIONALE_SEEN] = true }
    }

    suspend fun setBrightnessRationaleSeen() {
        dataStore.edit { it[Keys.BRIGHTNESS_RATIONALE_SEEN] = true }
    }

    val recentsFlow: Flow<List<String>> = dataStore.data.map { decodeRecent(it[Keys.RECENT_PACKAGES]) }

    suspend fun recordRecent(packageName: String) {
        dataStore.edit { prefs ->
            val list = decodeRecent(prefs[Keys.RECENT_PACKAGES])
            val updated = (listOf(packageName) + list.filter { it != packageName }).take(12)
            prefs[Keys.RECENT_PACKAGES] = encodeRecent(updated)
        }
    }

    suspend fun removeRecentMissing(installedPackages: Set<String>) {
        dataStore.edit { prefs ->
            val kept = decodeRecent(prefs[Keys.RECENT_PACKAGES]).filter { it in installedPackages }
            prefs[Keys.RECENT_PACKAGES] = encodeRecent(kept)
        }
    }

    private fun decodeRecent(raw: String?): List<String> =
        raw?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    private fun encodeRecent(list: List<String>): String = list.joinToString("\n")
}