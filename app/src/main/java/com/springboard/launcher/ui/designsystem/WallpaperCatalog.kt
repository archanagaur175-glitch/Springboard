package com.springboard.launcher.ui.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** A bundled, procedurally rendered wallpaper. High fidelity, zero binary assets in git. */
data class WallpaperSpec(
    val name: String,
    val colors: List<Color>,
    val angleDegrees: Float = 135f,
)

object WallpaperCatalog {
    val wallpapers: List<WallpaperSpec> = listOf(
        WallpaperSpec(
            name = "Iridescent",
            colors = listOf(Color(0xFFE9B7CE), Color(0xFF9B5DE5), Color(0xFF4E6FCE), Color(0xFF0A84FF)),
        ),
        WallpaperSpec(
            name = "Sunset",
            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF9F43), Color(0xFFFFD93D)),
        ),
        WallpaperSpec(
            name = "Midnight",
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        ),
        WallpaperSpec(
            name = "Ocean",
            colors = listOf(Color(0xFF0072FF), Color(0xFF00C6FF), Color(0xFF7FDBFF)),
        ),
        WallpaperSpec(
            name = "Aurora",
            colors = listOf(Color(0xFF00F5A0), Color(0xFF00D9F5), Color(0xFF7C3AED)),
        ),
        WallpaperSpec(
            name = "Berry",
            colors = listOf(Color(0xFF5F2A84), Color(0xFF7E2A9E), Color(0xFFB537A8)),
        ),
        WallpaperSpec(
            name = "Dusk",
            colors = listOf(Color(0xFF232526), Color(0xFF414345), Color(0xFF5A5F63)),
        ),
        WallpaperSpec(
            name = "Mono",
            colors = listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E), Color(0xFF3A3A3C)),
            angleDegrees = 90f,
        ),
    )

    fun brushFor(index: Int): Brush {
        val spec = wallpapers[index % wallpapers.size]
        val radians = Math.toRadians(spec.angleDegrees.toDouble())
        val cx = 0.5f
        val cy = 0.5f
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()
        val startX = cx - dx * 0.5f
        val startY = cy - dy * 0.5f
        val endX = cx + dx * 0.5f
        val endY = cy + dy * 0.5f
        return Brush.linearGradient(
            colors = spec.colors,
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(endX, endY),
        )
    }

    fun renderWallpaperBitmap(index: Int, width: Int, height: Int): Bitmap {
        val spec = wallpapers[index % wallpapers.size]
        val colors = spec.colors.map { it.toArgb() }.toIntArray()
        val radians = Math.toRadians(spec.angleDegrees.toDouble())
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()
        val x0 = width / 2f - dx * width * 0.7f
        val y0 = height / 2f - dy * height * 0.7f
        val x1 = width / 2f + dx * width * 0.7f
        val y1 = height / 2f + dy * height * 0.7f
        val shader = LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { this.shader = shader })
        return bitmap
    }
}

@Composable
fun rememberWallpaperBrush(index: Int): Brush {
    return remember(index) { WallpaperCatalog.brushFor(index) }
}

@Composable
fun WallpaperBackground(
    index: Int,
    modifier: Modifier = Modifier,
) {
    val brush = rememberWallpaperBrush(index)
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = brush) },
    )
}

val IosStatusBarHeight = 44.dp