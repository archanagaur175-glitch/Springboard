package com.springboard.launcher.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.data.db.FolderEntity
import com.springboard.launcher.data.db.GridItemEntity
import com.springboard.launcher.data.db.GridItemType
import com.springboard.launcher.domain.GridLayout
import com.springboard.launcher.ui.designsystem.IosRed
import kotlin.math.sin

/**
 * One page of the home grid: a fixed 4x6 arrangement of icon cells. Cell bounds are
 * registered with the shared [DragController] so jiggle drags can hit-test against
 * real on-screen rectangles, highlight targets, and resolve drops on release.
 */
@Composable
fun GridPage(
    page: Int,
    items: List<GridItemEntity>,
    installed: Map<String, InstalledApp>,
    folders: Map<Long, FolderEntity>,
    folderNames: Map<Long, String>,
    appRepository: AppRepository,
    jiggle: Boolean,
    dragController: DragController,
    onTapApp: (String) -> Unit,
    onTapFolder: (Long) -> Unit,
    onLongPressItem: (GridItemEntity) -> Unit,
    onRemoveItem: (GridItemEntity) -> Unit,
    onSwapItems: (GridItemEntity, GridItemEntity) -> Unit,
    onMoveToSlot: (GridItemEntity, Int) -> Unit,
    onDropInFolder: (GridItemEntity, Long) -> Unit,
    onDropOnDock: (GridItemEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "jiggle")
    val wobbleRad by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "wobble",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        for (row in 0 until GridLayout.ROWS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                for (col in 0 until GridLayout.COLUMNS) {
                    val slotInPage = row * GridLayout.COLUMNS + col
                    val absoluteIndex = GridLayout.indexOf(page, slotInPage)
                    val item = items.getOrNull(slotInPage)
                    val squircle = com.springboard.launcher.ui.designsystem.rememberSquircleShape()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(3.dp)
                            .onGloballyPositioned { coords ->
                                val rect = Rect(coords.positionInWindow(), coords.size.toSize())
                                if (item != null) {
                                    if (item.isFolder) {
                                        dragController.registerFolderBound(item.refKey, rect)
                                    } else {
                                        dragController.registerAppBound(item.refKey, rect)
                                    }
                                }
                                // The occupied cell also counts as a (no-op) empty drop target
                                // so a drop back on the origin cell resolves to "nothing".
                                dragController.registerEmptySlot(absoluteIndex, rect)
                            },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (item != null) {
                            IconCell(
                                item = item,
                                cellIndex = absoluteIndex,
                                items = items,
                                installed = installed,
                                folders = folders,
                                folderNames = folderNames,
                                appRepository = appRepository,
                                jiggle = jiggle,
                                wobbleRad = wobbleRad,
                                dragController = dragController,
                                onTapApp = onTapApp,
                                onTapFolder = onTapFolder,
                                onLongPressItem = onLongPressItem,
                                onRemoveItem = onRemoveItem,
                                onSwapItems = onSwapItems,
                                onMoveToSlot = onMoveToSlot,
                                onDropInFolder = onDropInFolder,
                                onDropOnDock = onDropOnDock,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (
                            jiggle &&
                            dragController.isDragging &&
                            dragController.hoveredEmptySlotIndex == absoluteIndex
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .clip(squircle)
                                    .border(2.dp, Color.White.copy(alpha = 0.55f), squircle),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconCell(
    item: GridItemEntity,
    cellIndex: Int,
    items: List<GridItemEntity>,
    installed: Map<String, InstalledApp>,
    folders: Map<Long, FolderEntity>,
    folderNames: Map<Long, String>,
    appRepository: AppRepository,
    jiggle: Boolean,
    wobbleRad: Float,
    dragController: DragController,
    onTapApp: (String) -> Unit,
    onTapFolder: (Long) -> Unit,
    onLongPressItem: (GridItemEntity) -> Unit,
    onRemoveItem: (GridItemEntity) -> Unit,
    onSwapItems: (GridItemEntity, GridItemEntity) -> Unit,
    onMoveToSlot: (GridItemEntity, Int) -> Unit,
    onDropInFolder: (GridItemEntity, Long) -> Unit,
    onDropOnDock: (GridItemEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = if (item.isApp) installed[item.refKey] else null
    val folderId = if (item.isFolder) item.refKey.toLongOrNull() else null
    val phase = (item.id * 137.5f) % 360f
    val rotation = if (jiggle) sin(Math.toRadians((wobbleRad + phase).toDouble())).toFloat() * 2.8f else 0f

    val isHovered = dragController.isDragging && dragController.hoveringKey == item.refKey
    val isGhost = dragController.isDragging && dragController.draggingKey == item.refKey

    val onTap = {
        when (item.type) {
            GridItemType.APP -> app?.let { onTapApp(it.packageName) }
            GridItemType.FOLDER -> folderId?.let { onTapFolder(it) }
        }
    }
    val onRemove = {
        when (item.type) {
            GridItemType.APP -> app?.let { onRemoveItem(item) }
            GridItemType.FOLDER -> folderId?.let { onRemoveItem(item) }
        }
    }

    var nodeCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val resolveDrop: () -> Unit = {
        val hit = dragController.endDrag()
        when {
            hit.key == null && hit.slotIndex != null -> onMoveToSlot(item, hit.slotIndex)
            hit.key != null && hit.isFolder -> hit.key.toLongOrNull()?.let { onDropInFolder(item, it) }
            hit.key != null -> items.firstOrNull { it.refKey == hit.key }?.let { target -> onSwapItems(item, target) }
            dragController.goingToDock() -> onDropOnDock(item)
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                if (isHovered) {
                    scaleX = 1.1f
                    scaleY = 1.1f
                }
                if (isGhost) alpha = 0.25f
            }
            .onGloballyPositioned { coords -> nodeCoords = coords }
            .pointerInput(jiggle, dragController) {
                val nodeTopLeft = { nodeCoords?.positionInWindow() }
                if (jiggle) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val topLeft = nodeTopLeft()
                            if (topLeft != null) {
                                dragController.beginDrag(item.id, item.refKey, item.type, topLeft + start)
                            }
                        },
                        onDrag = { change, delta ->
                            val topLeft = nodeTopLeft()
                            val window = if (topLeft != null) topLeft + change.position else change.position
                            dragController.move(delta, window)
                        },
                        onDragEnd = { resolveDrop() },
                        onDragCancel = { dragController.cancelDrag() },
                    )
                } else {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPressItem(item) },
                    )
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 6.dp),
        ) {
            val label = when {
                item.isApp -> app?.label ?: item.refKey
                item.isFolder -> folderNames[folderId] ?: "Folder"
                else -> ""
            }
            Box(contentAlignment = Alignment.Center) {
                when {
                    app != null -> AppIconView(
                        app = app,
                        appRepository = appRepository,
                        modifier = Modifier.size(50.dp),
                    )
                    folderId != null -> FolderTile(
                        folderId = folderId,
                        folders = folders,
                        installed = installed,
                        appRepository = appRepository,
                        modifier = Modifier.size(50.dp),
                    )
                    else -> Box(
                        Modifier
                            .size(50.dp)
                            .clip(com.springboard.launcher.ui.designsystem.rememberSquircleShape())
                            .background(Color.White.copy(alpha = 0.06f)),
                    )
                }
                if (jiggle && !isGhost) {
                    DeleteBadge(
                        onClick = { onRemove() },
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun DeleteBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(IosRed)
            .pointerInput(onClick) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun FolderTile(
    folderId: Long,
    folders: Map<Long, FolderEntity>,
    installed: Map<String, InstalledApp>,
    appRepository: AppRepository,
    modifier: Modifier = Modifier,
) {
    val folder = folders[folderId]
    Box(
        modifier = modifier
            .clip(com.springboard.launcher.ui.designsystem.rememberSquircleShape())
            .background(Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = folder?.name?.firstOrNull()?.uppercase() ?: "F",
            color = Color.White,
            fontSize = 18.sp,
        )
    }
}