package com.springboard.launcher.data.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.MediaStore
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Queries installed launchable apps, live-updates via package add/remove/replace
 * broadcasts, and caches per-package icon bitmaps (keyed by package + icon version)
 * so grid scrolling stays smooth.
 */
class AppRepository(
    private val context: Context,
    private val iconSizePx: Int,
    private val scope: CoroutineScope,
    private val onPackagesChanged: (List<String>) -> Unit,
) {
    private val packageManager: PackageManager = context.packageManager

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val iconCache = object : LruCache<String, ImageBitmap>(72) {}

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshAsync()
        }
    }

    init {
        refreshAsync()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun refreshAsync() {
        scope.launch {
            val fresh = withContext(Dispatchers.IO) { queryApps() }
            _apps.value = fresh
            onPackagesChanged(fresh.map { it.packageName })
        }
    }

    private fun queryApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (t: Throwable) {
            emptyList()
        }
        val apps = resolved.mapNotNull { ri ->
            val info = ri.activityInfo ?: return@mapNotNull null
            if (!info.applicationInfo.enabled) return@mapNotNull null
            val label = info.loadLabel(packageManager)?.toString()?.trim()
                ?: info.packageName
            if (label.isEmpty()) return@mapNotNull null
            InstalledApp(
                packageName = info.packageName,
                label = label,
                iconVersion = runCatching { packageManager.getPackageInfo(info.packageName, 0).lastUpdateTime }
                    .getOrDefault(0L),
            )
        }
        return apps
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase(Locale.getDefault()) }, { it.packageName }))
    }

    suspend fun loadIcon(app: InstalledApp): ImageBitmap? = loadIcon(app.packageName, app.iconVersion)

    suspend fun loadIcon(packageName: String, iconVersion: Long): ImageBitmap? {
        val key = "$packageName:$iconVersion"
        iconCache.get(key)?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val drawable = try {
                packageManager.getApplicationIcon(packageName)
            } catch (t: Throwable) {
                return@withContext null
            }
            IconRenderer.render(drawable, iconSizePx).asImageBitmap()
        } ?: return null
        iconCache.put(key, loaded)
        return loaded
    }

    fun appFor(packageName: String): InstalledApp? = _apps.value.firstOrNull { it.packageName == packageName }

    fun launchIntent(packageName: String): Intent? =
        try {
            packageManager.getLaunchIntentForPackage(packageName)
        } catch (t: Throwable) {
            null
        }

    /** Finds the camera app's launcher intent so the lock screen can open it without CAMERA. */
    fun cameraLaunchIntent(): Intent? {
        return try {
            val resolveInfo = packageManager.resolveActivity(
                Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                PackageManager.MATCH_DEFAULT_ONLY,
            ) ?: return null
            val cameraPackage = resolveInfo.activityInfo.packageName
            packageManager.getLaunchIntentForPackage(cameraPackage)
        } catch (t: Throwable) {
            null
        }
    }
}