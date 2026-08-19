package com.springboard.launcher.ui.designsystem

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * iOS-style frosted glass surface. On API 31+ the backdrop brush (the same gradient the
 * launcher paints as wallpaper) is drawn behind a GPU blur via RenderEffect; below API 31
 * a tuned translucent scrim is used instead so the material still reads as coherent glass.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backdrop: Brush = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.35f)),
    ),
    tint: Color = Color.Black.copy(alpha = 0.16f),
    blurRadius: Float = 36f,
    content: @Composable BoxScope.() -> Unit,
) {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (canBlur) {
                        renderEffect = RenderEffect.createBlurEffect(
                            blurRadius,
                            blurRadius,
                            android.graphics.Shader.TileMode.CLAMP,
                        )
                    }
                }
                .drawBehind {
                    drawRect(brush = backdrop)
                },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(color = tint)
                },
        )
        content()
    }
}