package com.springboard.launcher.systemui

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Owns the real system bar visibility inside Springboard-owned surfaces. The launcher
 * and lock facade run fully edge-to-edge with the OS status bar and navigation bar hidden
 * so Springboard can render its own iOS-style status bar and home indicator. The OS bars
 * remain fully intact on every other surface (settings, other apps) — nothing is disabled.
 */
object StatusBarController {

    fun hideBars(activity: Activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun showBars(activity: Activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}