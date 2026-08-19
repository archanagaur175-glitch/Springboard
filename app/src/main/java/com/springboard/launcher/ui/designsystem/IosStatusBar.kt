package com.springboard.launcher.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.springboard.launcher.data.system.SystemStateRepository

/**
 * Springboard's own iOS-style status bar, rendered inside every Springboard-owned surface.
 * The real status bar is hidden on those surfaces (see StatusBarController); this one draws
 * live time + signal/wifi/battery and exposes two swipe zones:
 *  - left half swipe-down  -> Notification Center
 *  - right half swipe-down -> Control Center
 */
@Composable
fun IosStatusBar(
    state: SystemStateRepository,
    onOpenNotificationCenter: () -> Unit,
    onOpenControlCenter: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    val time by state.timeText.collectAsStateWithLifecycle(initialValue = "9:41")
    val battery by state.batteryLevel.collectAsStateWithLifecycle(initialValue = 100)
    val charging by state.isCharging.collectAsStateWithLifecycle(initialValue = false)
    val wifi by state.wifiLevel.collectAsStateWithLifecycle(initialValue = 4)
    val signal by state.signalLevel.collectAsStateWithLifecycle(initialValue = 4)

    val barPx = with(LocalDensity.current) { IosStatusBarHeight.toPx() }

    Row(modifier = modifier.fillMaxWidth()) {
        SwipeZone(
            onSwipeDown = onOpenNotificationCenter,
            modifier = Modifier
                .weight(1f)
                .height(IosStatusBarHeight)
                .systemGestureExclusion(Rect(0f, 0f, EXCLUSION_WIDTH, barPx)),
        ) {
            Text(
                text = time,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        SwipeZone(
            onSwipeDown = onOpenControlCenter,
            modifier = Modifier
                .weight(1f)
                .height(IosStatusBarHeight)
                .systemGestureExclusion(Rect(0f, 0f, EXCLUSION_WIDTH, barPx)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = if (wifi > 0) tint else tint.copy(alpha = 0.3f),
                    modifier = Modifier.size(15.dp),
                )
                SignalGlyph(
                    level = signal,
                    tint = tint,
                    modifier = Modifier.width(16.dp).height(11.dp),
                )
                BatteryGlyph(
                    level = battery,
                    charging = charging,
                    tint = tint,
                    modifier = Modifier.width(24.dp).height(11.dp),
                )
            }
        }
    }
}

private const val EXCLUSION_WIDTH = 10000f

@Composable
private fun SwipeZone(
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.pointerInput(onSwipeDown) {
            var total = 0f
            detectDragGestures(
                onDragStart = { total = 0f },
                onDrag = { _, dragAmount -> total += dragAmount.y },
                onDragEnd = {
                    if (total > 36f) onSwipeDown()
                },
                onDragCancel = {},
            )
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun BatteryGlyph(
    level: Int,
    charging: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val frameW = 22.dp
    val frameH = 10.dp
    val tipW = 2.dp
    Canvas(modifier = modifier) {
        val w = frameW.toPx()
        val h = frameH.toPx()
        val tip = tipW.toPx()
        val r = 2.dp.toPx()
        drawRoundRect(
            color = tint.copy(alpha = 0.7f),
            topLeft = Offset(w + 1.5.dp.toPx(), h / 4f),
            size = Size(tip, h / 2f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.85f),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(r),
            style = Stroke(width = 1.dp.toPx()),
        )
        val inset = 2.dp.toPx()
        val fillWidth = (w - inset * 2f) * (level.coerceIn(0, 100) / 100f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = if (charging) IosGreen else tint,
                topLeft = Offset(inset, inset),
                size = Size(fillWidth, h - inset * 2f),
                cornerRadius = CornerRadius(r * 0.6f),
            )
        }
    }
}

@Composable
fun SignalGlyph(
    level: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val barW = 2.6.dp
    val gap = 1.6.dp
    Canvas(modifier = modifier) {
        val bw = barW.toPx()
        val g = gap.toPx()
        for (i in 0..3) {
            val active = i < level
            val hh = (i + 1) * 2.2.dp.toPx()
            val x = i * (bw + g)
            drawRoundRect(
                color = if (active) tint else tint.copy(alpha = 0.3f),
                topLeft = Offset(x, size.height - hh),
                size = Size(bw, hh),
                cornerRadius = CornerRadius(0.8.dp.toPx()),
            )
        }
    }
}

/** Icon size used across home grid, dock, and library — shared for consistency. */
val AppIconSize: Dp = 56.dp