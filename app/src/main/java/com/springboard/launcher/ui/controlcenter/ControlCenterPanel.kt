package com.springboard.launcher.ui.controlcenter

import android.annotation.SuppressLint
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.data.system.SystemStateRepository
import com.springboard.launcher.ui.designsystem.GlassSurface
import com.springboard.launcher.ui.designsystem.IosBlue
import kotlin.math.max

/**
 * The simulated Control Center: a frosted strip that slides down from the top with
 * brightness and volume sliders, quick toggles (Wi-Fi, Bluetooth, Torch, Rotation,
 * DND, Airplane Mode), and a Now Playing tile fed by the active media session. Every
 * toggle drives the real system (or falls back to a settings deep-link when the API
 * blocks it), and the panel dismisses on swipe-up, tap-outside, or the close button.
 */
@SuppressLint("NewApi")
@Composable
fun ControlCenterPanel(
    systemState: SystemStateRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val brightness by systemState.brightnessPercent.collectAsState()
    val volume by systemState.volumePercent.collectAsState()
    val wifiOn by systemState.wifiEnabled.collectAsState()
    val btOn by systemState.bluetoothEnabled.collectAsState()
    val torchOn by systemState.torchOn.collectAsState()
    val media by systemState.mediaSession.collectAsState()

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var needsWriteSettings by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
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
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(28.dp)),
                blurRadius = 48f,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = systemState.timeText.collectAsState().value,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        ToggleChip(
                            label = "Close",
                            active = false,
                            onClick = onClose,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    SliderRow(
                        iconText = "Brightness",
                        value = brightness,
                        onValueChange = { v ->
                            if (Settings.System.canWrite(context)) {
                                systemState.setBrightnessPercent(v.toInt())
                            } else {
                                needsWriteSettings = true
                            }
                        },
                    )
                    SliderRow(
                        iconText = "Volume",
                        value = volume,
                        onValueChange = { v -> systemState.setVolumePercent(v.toInt()) },
                    )

                    if (needsWriteSettings) {
                        Text(
                            text = "Allow Settings access to control brightness",
                            color = IosBlue,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        context.startActivity(
                                            android.content.Intent(
                                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                android.net.Uri.parse("package:${context.packageName}"),
                                            ),
                                        )
                                        needsWriteSettings = false
                                    })
                                },
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(Modifier.fillMaxWidth()) {
                        ToggleChip(label = "Wi-Fi", active = wifiOn, onClick = {
                            systemState.setWifiEnabled(!wifiOn) {
                                runCatching {
                                    context.startActivity(android.content.Intent(Settings.Panel.ACTION_WIFI))
                                }
                            }
                        })
                        Spacer(Modifier.width(10.dp))
                        ToggleChip(label = "Bluetooth", active = btOn, onClick = {
                            systemState.setBluetoothEnabled(!btOn) {
                                runCatching {
                                    context.startActivity(android.content.Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                }
                            }
                        })
                        Spacer(Modifier.width(10.dp))
                        ToggleChip(label = "Torch", active = torchOn, onClick = { systemState.toggleTorch() })
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        RotationToggle()
                        Spacer(Modifier.width(10.dp))
                        ToggleChip(label = "DND", active = false, onClick = {
                            val nm = context.getSystemService(android.app.NotificationManager::class.java)
                            if (nm?.areNotificationsEnabled() == true) {
                                runCatching { nm?.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
                                    .onFailure {
                                        runCatching {
                                            context.startActivity(android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                        }
                                    }
                            } else {
                                runCatching {
                                    context.startActivity(android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                }
                            }
                        })
                        Spacer(Modifier.width(10.dp))
                        ToggleChip(label = "Airplane", active = false, onClick = {
                            if (Settings.System.canWrite(context)) {
                                val enabled = Settings.Global.getInt(
                                    context.contentResolver,
                                    Settings.Global.AIRPLANE_MODE_ON,
                                    0,
                                ) == 1
                                runCatching {
                                    Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (enabled) 0 else 1)
                                    val i = android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                                        putExtra("state", !enabled)
                                        flags = android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY
                                    }
                                    context.sendBroadcast(i)
                                }
                            } else {
                                runCatching {
                                    context.startActivity(android.content.Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                }
                            }
                        }
                    }

                    val session = media
                    if (session != null) {
                        Spacer(Modifier.height(18.dp))
                        NowPlayingTile(
                            title = session.title ?: "Now Playing",
                            artist = session.artist ?: session.packageName,
                            isPlaying = session.isPlaying,
                            onToggle = { systemState.toggleMediaPlayPause() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ToggleChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f))
            .pointerInput(label) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (active) IosBlue else Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.take(3).uppercase(),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.RotationToggle() {
    var rotationLocked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    ToggleChip(label = "Rotation", active = rotationLocked, onClick = {
        val canWrite = Settings.System.canWrite(context)
        if (canWrite) {
            rotationLocked = !rotationLocked
            runCatching {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    if (rotationLocked) 0 else 1,
                )
            }
        } else {
            runCatching {
                context.startActivity(
                    android.content.Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        android.net.Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
        }
    })
}

@Composable
private fun SliderRow(
    iconText: String,
    value: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = iconText,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            modifier = Modifier.width(76.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = 5f..100f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NowPlayingTile(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(12.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggle() })
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            text = if (isPlaying) "⏸" else "▶",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}