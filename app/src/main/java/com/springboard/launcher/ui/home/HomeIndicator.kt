package com.springboard.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val EXCLUSION_WIDTH = 10000f

/**
 * iOS-style home indicator pill drawn inside Springboard's own window. A swipe-up from the
 * pill opens the recents-style switcher. The pill's touch region is excluded from the
 * system's edge-back gesture so the two never compete for the same touches.
 */
@Composable
fun HomeIndicator(
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barHeight = 5.dp
    val zoneHeight = 34.dp
    val zonePx = with(LocalDensity.current) { zoneHeight.toPx() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(zoneHeight)
            .systemGestureExclusion { Rect(0f, 0f, EXCLUSION_WIDTH, zonePx) }
            .pointerInput(onSwipeUp) {
                var total = 0f
                detectDragGestures(
                    onDragStart = { total = 0f },
                    onDrag = { _, dragAmount -> total += dragAmount.y },
                    onDragEnd = { if (total < -40f) onSwipeUp() },
                    onDragCancel = {},
                )
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .width(120.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f)),
        )
    }
}