package com.springboard.launcher.systemui

import android.content.Context

/**
 * System-level overlay surface entry points. The full Control Center implementation
 * (System-alert-window panel + Tier 2 top strip) lands with the Control Center feature.
 */
object OverlayController {

    fun showControlCenter(context: Context) {
        OverlayService.startControlCenter(context.applicationContext)
    }

    fun dismissControlCenter() {
        OverlayService.dismissControlCenter()
    }

    /** Whether the system has granted android.permission.SYSTEM_ALERT_WINDOW. */
    fun canDrawOverlays(context: Context): Boolean =
        android.provider.Settings.canDrawOverlays(context)
}