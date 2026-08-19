package com.springboard.launcher.ui.lockscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.data.prefs.AppPrefs
import com.springboard.launcher.systemui.SpringboardNotificationListener
import com.springboard.launcher.ui.designsystem.WallpaperBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Springboard lock facade: wallpaper, a large live clock, date, quick actions
 * (torch and camera), a compact notification preview, and a spring swipe-up that
 * finishes the façade and hands off to the real unlock flow. It never authenticates.
 */
@Composable
fun LockScreen(
    app: SpringboardApp,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = app.container
    val context = LocalContext.current
    val time by container.systemState.timeText.collectAsState()
    val wallpaper by container.settings.wallpaperFlow.collectAsState(initial = AppPrefs.wallpaperIndex)
    val notifications by SpringboardNotificationListener.notifications.collectAsState(initial = emptyList())

    var swipeDelta by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackground(index = wallpaper, modifier = Modifier.matchParentSize())
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.22f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
        ) {
            Text(
                text = time,
                color = Color.White,
                fontSize = 88.sp,
                fontWeight = FontWeight.Thin,
                modifier = Modifier.padding(top = 110.dp),
            )
            Text(
                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(26.dp))
            Row {
                QuickAction(label = "Torch", onClick = { container.systemState.toggleTorch() })
                Spacer(Modifier.width(18.dp))
                QuickAction(label = "Camera", onClick = {
                    container.appRepository.cameraLaunchIntent()?.let {
                        runCatching { context.startActivity(it) }
                    }
                })
            }

            Spacer(Modifier.weight(1f))

            if (SpringboardNotificationListener.isConnected && !notifications.isNullOrEmpty()) {
                notifications.orEmpty().take(3).forEach { n ->
                    LockNotificationPreview(n)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(18.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .graphicsLayer { translationY = swipeDelta.coerceIn(0f, 240f) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                swipeDelta += dragAmount
                            },
                            onDragEnd = {
                                if (swipeDelta < -90f) {
                                    onUnlock()
                                } else {
                                    swipeDelta = 0f
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Swipe up to unlock",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.7f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .pointerInput(label) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LockNotificationPreview(notification: com.springboard.launcher.systemui.SpringboardNotification, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (notification.appLabel ?: notification.packageName).take(1).uppercase(),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = notification.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            val body = notification.bigText ?: notification.text
            if (body != null) {
                Text(
                    text = body,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }
        }
    }
}