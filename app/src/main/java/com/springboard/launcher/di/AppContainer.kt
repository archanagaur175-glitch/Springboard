package com.springboard.launcher.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.db.AppDatabase
import com.springboard.launcher.data.layout.HomeLayoutRepository
import com.springboard.launcher.data.prefs.SettingsRepository
import com.springboard.launcher.data.prefs.springboardDataStore
import com.springboard.launcher.data.system.SystemStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Hand-rolled service container. The app is small enough that explicit construction is
 * dramatically simpler and more CI-stable than a DI framework.
 */
class AppContainer(private val app: SpringboardApp) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(app, AppDatabase::class.java, AppDatabase.NAME).build()
    }

    private val dataStore: DataStore<Preferences> by lazy { app.springboardDataStore }

    val settings: SettingsRepository by lazy { SettingsRepository(dataStore) }

    val homeLayout: HomeLayoutRepository by lazy {
        HomeLayoutRepository(
            gridDao = database.gridItemDao(),
            folderDao = database.folderDao(),
            memberDao = database.folderMemberDao(),
            dockDao = database.dockItemDao(),
        )
    }

    val appRepository: AppRepository by lazy {
        AppRepository(
            context = app,
            iconSizePx = (app.resources.displayMetrics.density * 56f).roundToInt(),
            scope = appScope,
            onPackagesChanged = { packages ->
                appScope.launch {
                    homeLayout.syncInstalled(packages)
                }
                appScope.launch {
                    settings.removeRecentMissing(packages.toSet())
                }
            },
        )
    }

    val systemState: SystemStateRepository by lazy {
        SystemStateRepository(app, appScope)
    }

    fun bootstrap() {
        // Touch repositories so live state flows start immediately.
        appScope.launch { settings.hydrate() }
        appRepository
        systemState
    }
}