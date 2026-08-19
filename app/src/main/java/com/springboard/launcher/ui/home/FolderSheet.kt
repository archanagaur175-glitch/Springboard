package com.springboard.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.data.db.FolderMemberEntity
import com.springboard.launcher.data.db.GridItemType
import com.springboard.launcher.ui.designsystem.GlassSurface
import com.springboard.launcher.ui.designsystem.IosRed
import com.springboard.launcher.ui.designsystem.rememberSquircleShape
import kotlin.math.roundToInt

/**
 * Full folder sheet: a frosted panel showing the folder's member apps. Members can be
 * launched, renamed via the header field, removed through the X badge, or dragged back
 * out onto the home grid by releasing the drag beyond the panel edges.
 */
@Composable
fun FolderSheet(
    folderId: Long,
    folderName: String,
    members: List<FolderMemberEntity>,
    installed: Map<String, InstalledApp>,
    appRepository: AppRepository,
    jiggle: Boolean,
    onTapApp: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nameText by remember(folderId) { mutableStateOf(folderName) }
    var sheetJiggle by remember(folderId) { mutableStateOf(jiggle) }
    val keyboard = LocalSoftwareKeyboardController.current
    val memberDrag = remember { DragController() }
    var panelRect by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                },
        )

        GlassSurface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .height(300.dp)
                .clip(rememberSquircleShape())
                .onGloballyPositioned { coords ->
                    panelRect = Rect(coords.positionInWindow(), coords.size.toSize())
                },
            blurRadius = 48f,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onRename(nameText.trim().ifEmpty { "Folder" })
                                keyboard?.hide()
                            },
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Delete",
                        color = IosRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    onDelete()
                                    onClose()
                                })
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))

                if (members.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Drop apps here in jiggle mode",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxWidth().height(224.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(members, key = { it.packageName }) { member ->
                            val app = installed[member.packageName]
                            if (app != null) {
                                MemberCell(
                                    app = app,
                                    appRepository = appRepository,
                                    jiggle = sheetJiggle,
                                    dragController = memberDrag,
                                    panelBounds = { panelRect },
                                    onTap = { onTapApp(member.packageName) },
                                    onLongPress = { sheetJiggle = true },
                                    onRemove = { onRemoveMember(member.packageName) },
                                    onDragOut = { onRemoveMember(member.packageName) },
                                    modifier = Modifier.aspectRatio(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        val draggingKey = memberDrag.draggingKey
        if (draggingKey != null) {
            val ghostApp = if (memberDrag.draggingType == GridItemType.APP) installed[draggingKey] else null
            val ghostSizePx = with(LocalDensity.current) { 48.dp.toPx() }
            Box(
                Modifier
                    .fillMaxSize()
                    .offset {
                        val centerX = memberDrag.dragStartWindow.x + memberDrag.ghostOffset.x
                        val centerY = memberDrag.dragStartWindow.y + memberDrag.ghostOffset.y
                        IntOffset(
                            x = (centerX - ghostSizePx / 2f).roundToInt(),
                            y = (centerY - ghostSizePx / 2f).roundToInt(),
                        )
                    }
                    .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f },
            ) {
                if (ghostApp != null) {
                    AppIconView(
                        app = ghostApp,
                        appRepository = appRepository,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberCell(
    app: InstalledApp,
    appRepository: AppRepository,
    jiggle: Boolean,
    dragController: DragController,
    panelBounds: () -> Rect?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
    onDragOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nodeCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .graphicsLayer { if (jiggle) rotationZ = 3f }
            .onGloballyPositioned { nodeCoords = it }
            .pointerInput(jiggle) {
                val topLeft = { nodeCoords?.positionInWindow() }
                if (jiggle) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val tl = topLeft()
                            if (tl != null) {
                                dragController.beginDrag(-1L, app.packageName, GridItemType.APP, tl + start)
                            }
                        },
                        onDrag = { change, delta ->
                            val tl = topLeft()
                            val window = if (tl != null) tl + change.position else change.position
                            dragController.move(delta, window)
                        },
                        onDragEnd = {
                            val releasedOutside = panelBounds()?.let { !it.contains(dragController.pointerWindow) } ?: false
                            dragController.endDrag()
                            if (releasedOutside) onDragOut()
                        },
                        onDragCancel = { dragController.cancelDrag() },
                    )
                } else {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIconView(
            app = app,
            appRepository = appRepository,
            modifier = Modifier.size(48.dp),
        )
        if (jiggle) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(IosRed)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onRemove() })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}