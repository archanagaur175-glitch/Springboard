package com.springboard.launcher.systemui

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Hosts the System-alert-window surfaces: the Control Center panel and (Tier 2, opt-in)
 * the thin top strip that extends Control Center gestures into third-party apps.
 * Full implementation lives with the Control Center feature.
 */
class OverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun isRunning(): Boolean = instance != null
        private var instance: OverlayService? = null
    }
}