package com.springboard.launcher.ui.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.springboard.launcher.ui.designsystem.SpringSpecs
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import kotlin.math.roundToInt

/**
 * Custom pager with spring-physics page snapping. Pages are laid out in a single wide row
 * and panned with a GPU transform; on release the nearest page snaps home with a spring
 * (never a linear tween).
 */
@Composable
fun SpringPager(
    pageCount: Int,
    currentPage: Int,
    onPageSettled: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (pageIndex: Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageWidth = maxWidth
        val pageWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { pageWidth.toPx() }
        val offset = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        val maxOffset = ((pageCount - 1).coerceAtLeast(0)) * pageWidthPx

        LaunchedEffect(Unit) {
            offset.snapTo((currentPage.coerceIn(0, pageCount - 1)) * pageWidthPx)
        }
        LaunchedEffect(currentPage, pageWidthPx) {
            val target = currentPage.coerceIn(0, pageCount - 1) * pageWidthPx
            if ((offset.value - target) * (offset.value - target) > 1f) {
                offset.animateTo(target, SpringSpecs.Page)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = -offset.value },
        ) {
            repeat(pageCount.coerceAtLeast(0)) { index ->
                Box(
                    modifier = Modifier
                        .width(pageWidth)
                        .fillMaxHeight(),
                ) {
                    content(index)
                }
            }
        }

        if (pageCount > 1 && enabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageCount, pageWidthPx, enabled) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset.stop() },
                            onHorizontalDrag = { _, dragAmount ->
                                val next = (offset.value - dragAmount).coerceIn(0f, maxOffset)
                                offset.snapTo(next)
                            },
                            onDragEnd = {
                                val nearest = ((offset.value / pageWidthPx).roundToInt())
                                    .coerceIn(0, pageCount - 1)
                                val target = nearest * pageWidthPx
                                scope.launch { offset.animateTo(target, SpringSpecs.Page) }
                                onPageSettled(nearest)
                            },
                            onDragCancel = {
                                val nearest = ((offset.value / pageWidthPx).roundToInt())
                                    .coerceIn(0, pageCount - 1)
                                scope.launch { offset.animateTo(nearest * pageWidthPx, SpringSpecs.Page) }
                            },
                        )
                    },
            )
        }

        if (pageCount == 0) {
            content(0)
        }
    }
}