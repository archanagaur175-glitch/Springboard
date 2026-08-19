package com.springboard.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.data.db.GridItemEntity
import com.springboard.launcher.data.db.GridItemType
import com.springboard.launcher.data.prefs.AppPrefs
import com.springboard.launcher.domain.GridLayout
import com.springboard.launcher.systemui.OverlayController
import com.springboard.launcher.ui.applibrary.AppLibraryPage
import com.springboard.launcher.ui.designsystem.IosStatusBar
import com.springboard.launcher.ui.designsystem.WallpaperBackground
import com.springboard.launcher.ui.designsystem.rememberWallpaperBrush
import com.springboard.launcher.ui.designsystem.rememberSquircleShape
import com.springboard.launcher.ui.notifications.NotificationCenterSurface
import com.springboard.launcher.ui.permission.GateKind
import com.springboard.launcher.ui.permission.PermissionGate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Springboard home screen: wallpaper, iOS status bar, paged grid + App Library,
 * page dots, glass dock, home indicator, and the overlay surfaces (folder, NC, recents).
 * A single [DragController] is hoisted here so jiggle drags share one source of truth
 * across every grid page and the dock.
 */
@Composable
fun LauncherScreen(app: SpringboardApp) {
    val container = app.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dragController = remember { DragController() }

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
    var gate by remember { mutableStateOf(GateKind.NONE) }

    val backdrop = rememberWallpaperBrush(wallpaper)

    val openNotificationCenter: () -> Unit = {
        val enabled = runCatching {
            android.app.NotificationManager.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }.getOrDefault(false)
        if (enabled) {
            ncVisible = true
        } else {
            gate = GateKind.NOTIFICATION_LISTENER
        }
    }

    val openControlCenter: () -> Unit = {
        if (OverlayController.canDrawOverlays(context)) {
            OverlayController.showControlCenter(context)
        } else {
            gate = GateKind.OVERLAY
        }
    }

    LaunchedEffect(gate) {
        when (gate) {
            GateKind.OVERLAY -> container.settings.setOverlayRationaleSeen()
            GateKind.WRITE_SETTINGS -> container.settings.setBrightnessRationaleSeen()
            GateKind.NOTIFICATION_LISTENER -> container.settings.setNcRationaleSeen()
            else -> Unit
        }
    }

    val launchApp: (String) -> Unit = { pkg ->
        val intent = container.appRepository.launchIntent(pkg)
        if (intent != null) {
            runCatching { context.startActivity(intent) }
            scope.launch { container.settings.recordRecent(pkg) }
        }
    }

    val openFolder: (Long) -> Unit = { id -> openFolderId = id }

    val removeItem: (GridItemEntity) -> Unit = { item ->
        scope.launch {
            when {
                item.isApp -> container.homeLayout.removeFromGrid(item.refKey)
                item.isFolder -> item.refKey.toLongOrNull()?.let { container.homeLayout.removeFolder(it) }
            }
            jiggleOn = false
        }
    }

    val swapItems: (GridItemEntity, GridItemEntity) -> Unit = { a, b ->
        scope.launch { container.homeLayout.swapGridPositions(a, b) }
    }

    val moveToSlot: (GridItemEntity, Int) -> Unit = { item, slotIndex ->
        scope.launch { container.homeLayout.moveGridItemTo(item, slotIndex) }
    }

    val dropInFolder: (GridItemEntity, Long) -> Unit = { item, folderId ->
        if (item.isApp) {
            scope.launch { container.homeLayout.addToFolder(folderId, item.refKey) }
        }
    }

    val dropOnDock: (GridItemEntity) -> Unit = { item ->
        if (item.isApp) {
            val pkg = item.refKey
            scope.launch {
                if (!container.homeLayout.isInDock(pkg)) {
                    container.homeLayout.addToDock(pkg)
                    container.homeLayout.removeFromGrid(pkg)
                }
            }
        }
    }

    // Dragging one app onto another for a beat merges them into a new folder.
    LaunchedEffect(dragController.draggingKey, dragController.hoveringKey, dragController.hoveringIsFolder) {
        val dragged = dragController.draggingKey ?: return@LaunchedEffect
        val target = dragController.hoveringKey ?: return@LaunchedEffect
        if (target == dragged || dragController.hoveringIsFolder) return@LaunchedEffect
        if (grid.none { it.refKey == dragged && it.isApp }) return@LaunchedEffect
        if (grid.none { it.refKey == target && it.isApp }) return@LaunchedEffect
        delay(650)
        if (dragController.draggingKey == dragged && dragController.hoveringKey == target) {
            val newFolderId = container.homeLayout.createFolder(dragged, target)
            openFolderId = newFolderId
            dragController.cancelDrag()
        }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(index = wallpaper, modifier = Modifier.matchParentSize())

        Column(Modifier.fillMaxSize()) {
            IosStatusBar(
                state = container.systemState,
                onOpenNotificationCenter = openNotificationCenter,
                onOpenControlCenter = openControlCenter,
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
                            page = page,
                            items = grid.filter { it.page == page }.sortedBy { it.slot },
                            installed = installedMap,
                            folders = folderMap,
                            folderNames = folderNames,
                            appRepository = container.appRepository,
                            jiggle = jiggleOn,
                            dragController = dragController,
                            onTapApp = launchApp,
                            onTapFolder = openFolder,
                            onLongPressItem = { jiggleOn = true },
                            onRemoveItem = removeItem,
                            onSwapItems = swapItems,
                            onMoveToSlot = moveToSlot,
                            onDropInFolder = dropInFolder,
                            onDropOnDock = dropOnDock,
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
                dragController = dragController,
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
            val folderMembers by remember(openFolderIdValue) {
                container.homeLayout.folderMembers(openFolderIdValue)
            }.collectAsStateWithLifecycle(initialValue = emptyList())
            FolderSheet(
                folderId = openFolderIdValue,
                folderName = folderNames[openFolderIdValue] ?: "Folder",
                members = folderMembers,
                installed = installedMap,
                appRepository = container.appRepository,
                jiggle = false,
                onTapApp = launchApp,
                onRemoveMember = { pkg ->
                    scope.launch { container.homeLayout.removeFromFolder(openFolderIdValue, pkg) }
                },
                onRename = { name ->
                    scope.launch { container.homeLayout.renameFolder(openFolderIdValue, name) }
                },
                onDelete = {
                    scope.launch { container.homeLayout.removeFolder(openFolderIdValue) }
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

        if (gate != GateKind.NONE) {
            PermissionGate(
                kind = gate,
                onOpenSettings = { intent ->
                    runCatching { context.startActivity(intent) }
                },
                onDismiss = { gate = GateKind.NONE },
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

        // Floating ghost of the item currently being dragged (grid + dock path only).
        val ghostKey = dragController.draggingKey
        if (ghostKey != null && openFolderId == null) {
            val ghostSize = 56.dp
            val ghostSizePx = with(LocalDensity.current) { ghostSize.toPx() }
            val ghostApp = if (dragController.draggingType == GridItemType.APP) installedMap[ghostKey] else null
            val ghostFolderId = if (dragController.draggingType == GridItemType.FOLDER) ghostKey.toLongOrNull() else null
            val ghostFolder = ghostFolderId?.let { folderMap[it] }
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = dragController.dragStartWindow.x + dragController.ghostOffset.x - ghostSizePx / 2f
                        translationY = dragController.dragStartWindow.y + dragController.ghostOffset.y - ghostSizePx / 2f
                        scaleX = 1.15f
                        scaleY = 1.15f
                    },
            ) {
                when {
                    ghostApp != null -> AppIconView(
                        app = ghostApp,
                        appRepository = container.appRepository,
                        modifier = Modifier.size(ghostSize),
                    )
                    ghostFolder != null -> Box(
                        Modifier
                            .size(ghostSize)
                            .clip(rememberSquircleShape())
                            .background(Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Text(
                            text = ghostFolder.name.firstOrNull()?.uppercase() ?: "F",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}