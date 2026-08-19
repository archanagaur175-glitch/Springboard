package com.springboard.launcher.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.data.db.GridItemType
import com.springboard.launcher.data.prefs.AppPrefs
import com.springboard.launcher.domain.GridLayout
import com.springboard.launcher.systemui.OverlayController
import com.springboard.launcher.ui.applibrary.AppLibraryPage
import com.springboard.launcher.ui.designsystem.IosStatusBar
import com.springboard.launcher.ui.designsystem.WallpaperBackground
import com.springboard.launcher.ui.designsystem.rememberWallpaperBrush
import com.springboard.launcher.ui.notifications.NotificationCenterSurface
import kotlinx.coroutines.launch

/**
 * The Springboard home screen: wallpaper, iOS status bar, paged grid + App Library,
 * page dots, glass dock, home indicator, and the overlay surfaces (folder, NC, recents).
 */
@Composable
fun LauncherScreen(app: SpringboardApp) {
    val container = app.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val installed by container.appRepository.apps.collectAsStateWithLifecycle()
    val grid by container.homeLayout.grid.collectAsStateWithLifecycle()
    val dock by container.homeLayout.dock.collectAsStateWithLifecycle()
    val folders by container.homeLayout.folders.collectAsStateWithLifecycle()
    val recents by container.settings.recentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val wallpaper by container.settings.wallpaperFlow.collectAsStateWithLifecycle(initialValue = AppPrefs.wallpaperIndex)

    val installedMap = remember(installed) { installed.associateBy { it.packageName } }
    val folderMap = remember(folders) { folders.associateBy { it.id } }
    val folderNames = remember(folders) { folders.associate { it.id to it.name } }

    val gridPageCount = GridLayout.pageCount(grid.size)
    val totalPages = gridPageCount + 1

    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var jiggleOn by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<Long?>(null) }
    var ncVisible by remember { mutableStateOf(false) }
    var recentsVisible by remember { mutableStateOf(false) }

    val backdrop = rememberWallpaperBrush(wallpaper)

    val launchApp: (String) -> Unit = { pkg ->
        val intent = container.appRepository.launchIntent(pkg)
        if (intent != null) {
            runCatching { context.startActivity(intent) }
            scope.launch { container.settings.recordRecent(pkg) }
        }
    }

    val openFolder: (Long) -> Unit = { id -> openFolderId = id }

    val removeItem: (com.springboard.launcher.data.db.GridItemEntity) -> Unit = { item ->
        scope.launch {
            when (item.type) {
                GridItemType.APP -> container.homeLayout.removeFromGrid(item.refKey)
                GridItemType.FOLDER -> item.refKey.toLongOrNull()?.let { container.homeLayout.removeFolder(it) }
            }
            jiggleOn = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(index = wallpaper, modifier = Modifier.matchParentSize())

        Column(Modifier.fillMaxSize()) {
            IosStatusBar(
                state = container.systemState,
                onOpenNotificationCenter = { ncVisible = true },
                onOpenControlCenter = {
                    if (OverlayController.canDrawOverlays(context)) {
                        OverlayController.showControlCenter(context)
                    }
                },
            )

            Box(Modifier.weight(1f)) {
                SpringPager(
                    pageCount = totalPages,
                    currentPage = currentPage,
                    onPageSettled = { page ->
                        currentPage = page
                        scope.launch { container.settings.setCurrentPage(page) }
                    },
                    enabled = !jiggleOn && openFolderId == null,
                ) { page ->
                    if (page < gridPageCount) {
                        GridPage(
                            items = grid.filter { it.page == page }.sortedBy { it.slot },
                            installed = installedMap,
                            folders = folderMap,
                            folderNames = folderNames,
                            appRepository = container.appRepository,
                            jiggle = jiggleOn,
                            onTapApp = launchApp,
                            onTapFolder = openFolder,
                            onLongPressItem = { jiggleOn = true },
                            onRemoveItem = removeItem,
                            dragHooks = remember { DragController() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        AppLibraryPage(
                            apps = installed,
                            appRepository = container.appRepository,
                            onTapApp = launchApp,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            PageDots(
                count = gridPageCount,
                current = currentPage.coerceIn(0, gridPageCount - 1),
                onSelect = { page ->
                    currentPage = page
                    scope.launch { container.settings.setCurrentPage(page) }
                },
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Dock(
                items = dock,
                installed = installedMap,
                appRepository = container.appRepository,
                backdrop = backdrop,
                onTap = launchApp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )

            HomeIndicator(
                onSwipeUp = { if (!jiggleOn && openFolderId == null) recentsVisible = true },
                modifier = Modifier.navigationBarsPadding().padding(top = 2.dp),
            )
        }

        val openFolderIdValue = openFolderId
        if (openFolderIdValue != null) {
            FolderSheet(
                folderId = openFolderIdValue,
                folderName = folderNames[openFolderIdValue] ?: "Folder",
                members = emptyList(),
                installed = installedMap,
                appRepository = container.appRepository,
                systemState = container.systemState,
                jiggle = false,
                onTapApp = launchApp,
                onRemoveMember = { pkg ->
                    scope.launch { container.homeLayout.removeFromFolder(openFolderIdValue, pkg) }
                },
                onClose = { openFolderId = null },
                modifier = Modifier.matchParentSize(),
            )
        }

        if (ncVisible) {
            NotificationCenterSurface(
                onClose = { ncVisible = false },
                systemState = container.systemState,
                appRepository = container.appRepository,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (recentsVisible) {
            RecentsSwitcher(
                onClose = { recentsVisible = false },
                recentPackages = recents,
                appRepository = container.appRepository,
                systemState = container.systemState,
                onLaunch = launchApp,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (jiggleOn && openFolderId == null) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { jiggleOn = false })
                    },
            )
        }
    }
}