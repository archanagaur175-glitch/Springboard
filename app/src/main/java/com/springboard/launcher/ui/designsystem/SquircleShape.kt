package com.springboard.launcher.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * A true superellipse squircle used for every icon, dock tile and folder badge.
 * Edges stay straight while each corner follows the curve |x/r|^n + |y/r|^n = 1
 * (n = [curvature], default 5, which matches the iOS icon squircle closely).
 */
class SquircleShape(
    private val cornerRadiusFraction: Float = 0.5f,
    private val curvature: Float = 5f,
    private val stepsPerCorner: Int = 40,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rect = Rect(Offset.Zero, size)
        return Outline.Generic(superellipse(rect))
    }

    private fun superellipse(rect: Rect): Path {
        val w = rect.width
        val h = rect.height
        val r = min(w, h) * cornerRadiusFraction
        val left = rect.left
        val top = rect.top
        val n = curvature
        val halfPi = PI / 2.0

        val path = Path()
        path.moveTo(left + r, top)

        path.lineTo(left + w - r, top)
        corner(path, left + w - r, top + r, 1f, -1f, r, n, halfPi, 0.0, stepsPerCorner)

        path.lineTo(left + w, top + h - r)
        corner(path, left + w - r, top + h - r, 1f, 1f, r, n, 0.0, halfPi, stepsPerCorner)

        path.lineTo(left + r, top + h)
        corner(path, left + r, top + h - r, -1f, 1f, r, n, halfPi, 0.0, stepsPerCorner)

        path.lineTo(left, top + r)
        corner(path, left + r, top + r, -1f, -1f, r, n, 0.0, halfPi, stepsPerCorner)

        path.close()
        return path
    }

    private fun corner(
        path: Path,
        centerX: Float,
        centerY: Float,
        dirX: Float,
        dirY: Float,
        radius: Float,
        n: Float,
        fromPhi: Double,
        toPhi: Double,
        steps: Int,
    ) {
        val expo = 2f / n
        for (i in 1..steps) {
            val phi = fromPhi + (toPhi - fromPhi) * i / steps
            val dx = dirX * radius * cos(phi).toFloat().pow(expo)
            val dy = dirY * radius * sin(phi).toFloat().pow(expo)
            path.lineTo(centerX + dx, centerY + dy)
        }
    }
}

@Composable
fun rememberSquircleShape(cornerRadiusFraction: Float = 0.5f): Shape {
    return remember(cornerRadiusFraction) { SquircleShape(cornerRadiusFraction) }
}