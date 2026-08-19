package com.springboard.launcher.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.data.db.DockItemEntity
import com.springboard.launcher.ui.designsystem.GlassSurface

/**
 * Persistent glass dock, fixed across pages, holding the pinned app set. The dock
 * registers its window bounds with the shared [DragController] so a jiggle drag
 * released over the dock pins the app here. It lifts slightly under a hovered drag
 * so the drop destination is obvious.
 */
@Composable
fun Dock(
    items: List<DockItemEntity>,
    installed: Map<String, InstalledApp>,
    appRepository: AppRepository,
    backdrop: Brush,
    dragController: DragController,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockScale = if (dragController.isDragging && dragController.goingToDock()) 1.04f else 1f
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .graphicsLayer { scaleX = dockScale; scaleY = dockScale }
            .clip(RoundedCornerShape(28.dp))
            .onGloballyPositioned { coords ->
                dragController.registerDock(Rect(coords.positionInWindow(), coords.size))
            },
        backdrop = backdrop,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.sortedBy { it.slot }.take(6).forEach { item ->
                val app = installed[item.packageName]
                if (app != null) {
                    val isHovered = dragController.isDragging && dragController.hoveringKey == item.packageName
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .graphicsLayer {
                                if (isHovered) {
                                    scaleX = 1.12f
                                    scaleY = 1.12f
                                }
                            }
                            .pointerInput(item.packageName) {
                                detectTapGestures(onTap = { onTap(item.packageName) })
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIconView(
                            app = app,
                            appRepository = appRepository,
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }
            }
        }
    }
}