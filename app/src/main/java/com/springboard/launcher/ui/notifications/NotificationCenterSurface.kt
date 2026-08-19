package com.springboard.launcher.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.system.SystemStateRepository
import com.springboard.launcher.systemui.SpringboardNotification
import com.springboard.launcher.systemui.SpringboardNotificationListener
import com.springboard.launcher.ui.designsystem.GlassSurface
import kotlin.math.max

/**
 * The Notification Center: a frosted panel that slides down from the top, fed by the
 * NLS. Notifications can be opened (content intent), swiped-away one by one, or cleared
 * in bulk; the panel dismisses on swipe-down, tap-outside, or Close.
 */
@Composable
fun NotificationCenterSurface(
    onClose: () -> Unit,
    systemState: SystemStateRepository,
    appRepository: AppRepository,
    modifier: Modifier = Modifier,
) {
    val notifications by SpringboardNotificationListener.notifications.collectAsState(initial = emptyList())
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = max(0f, dragOffset) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragOffset > 160f) onClose() else dragOffset = 0f
                        },
                    )
                },
        ) {
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .size(width = 64.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .align(Alignment.CenterHorizontally),
            )

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp, top = 10.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(28.dp)),
                blurRadius = 48f,
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Notifications",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        if (notifications.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { SpringboardNotificationListener.dismissAll() })
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = "Close",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { onClose() })
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    if (notifications.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (SpringboardNotificationListener.isConnected) {
                                    "No notifications"
                                } else {
                                    "Enable notification access in Settings"
                                },
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(notifications, key = { it.key }) { n ->
                                NotificationRow(
                                    notification = n,
                                    appRepository = appRepository,
                                    onOpen = {
                                        runCatching { n.contentIntent?.send() }
                                    },
                                    onDismiss = { SpringboardNotificationListener.dismiss(n.key) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: SpringboardNotification,
    appRepository: AppRepository,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = remember(notification.packageName) {
        appRepository.appFor(notification.packageName)
    }
    val icon by produceState<ImageBitmap?>(null, notification.packageName) {
        value = appRepository.loadIcon(notification.packageName, app?.iconVersion ?: 0L)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .pointerInput(notification.key) {
                detectTapGestures(onTap = { onOpen() })
            }
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = notification.appLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.appLabel ?: notification.packageName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(notification.whenPosted),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = notification.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val body = notification.bigText ?: notification.text
            if (body != null) {
                Text(
                    text = body,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (notification.canClear) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .pointerInput(notification.key) {
                        detectTapGestures(onTap = { onDismiss() })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

private fun relativeTime(posted: Long): String {
    val minutes = (System.currentTimeMillis() - posted) / 60_000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        else -> "${minutes / 1440}d"
    }
}