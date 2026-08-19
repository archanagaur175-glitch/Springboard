package com.springboard.launcher.data.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/**
 * Renders an app icon into a fixed-size bitmap so Springboard can apply its own
 * squircle mask uniformly. Adaptive icons are rendered from their masked foreground
 * layer (content sits inside the 66/108 safe zone); legacy drawables are drawn
 * centered at 88% so the squircle crop doesn't bite into the badge.
 */
object IconRenderer {

    fun render(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (drawable is AdaptiveIconDrawable) {
            val foreground = drawable.foreground
            foreground?.let {
                it.setBounds(0, 0, sizePx, sizePx)
                it.draw(canvas)
            }
        } else {
            val inset = (sizePx * 0.06f).toInt()
            drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
            drawable.draw(canvas)
        }
        return bitmap
    }
}