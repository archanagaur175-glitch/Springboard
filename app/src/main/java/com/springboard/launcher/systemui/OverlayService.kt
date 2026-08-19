package com.springboard.launcher.systemui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Hosts the System-alert-window surfaces. The full Control Center implementation
 * (panel + Tier 2 top strip) lands with the Control Center feature.
 */
class OverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private var instance: OverlayService? = null

        fun startControlCenter(context: Context) {
            context.startService(Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_CC
            })
        }

        fun dismissControlCenter() {
            instance?.dismissPanel()
        }

        const val ACTION_SHOW_CC = "com.springboard.launcher.action.SHOW_CC"
        const val ACTION_GUARD = "com.springboard.launcher.action.GUARD"
    }

    private fun dismissPanel() {
        // Implemented with the Control Center feature.
    }
}