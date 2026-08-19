package com.springboard.launcher.systemui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.ui.controlcenter.ControlCenterPanel
import com.springboard.launcher.ui.designsystem.SpringboardTheme

/**
 * Hosts Springboard's system-alert-window surfaces. The Control Center is shown as a
 * full-screen translucent window whose top strip is drawn by compose; the window dims
 * the backdrop so the panel reads as the Control Center pull-down. The service keeps a
 * single instance so the launcher, the lock facade and the Tier 2 strip can all hand
 * off show/dismiss without leaking windows.
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        removeView()
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_CC -> showControlCenter()
            ACTION_HIDE -> dismissPanel()
        }
        return START_NOT_STICKY
    }

    private fun showControlCenter() {
        if (rootView != null) return
        val app = applicationContext as? SpringboardApp ?: return
        val composeView = ComposeView(this).apply {
            setContent {
                SpringboardTheme {
                    ControlCenterPanel(
                        systemState = app.container.systemState,
                        onClose = { dismissPanel() },
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT,
        ).apply {
            dimAmount = 0.35f
            gravity = Gravity.TOP
        }

        try {
            windowManager = getSystemService(WindowManager::class.java)
            windowManager?.addView(composeView, params)
            rootView = composeView
        } catch (t: Throwable) {
            rootView = null
        }
    }

    fun dismissPanel() {
        removeView()
    }

    private fun removeView() {
        val view = rootView
        if (view == null) return
        runCatching { windowManager?.removeView(view) }
        rootView = null
    }

    companion object {
        private var instance: OverlayService? = null

        fun startControlCenter(context: Context) {
            runCatching {
                context.startService(Intent(context, OverlayService::class.java).apply {
                    action = ACTION_SHOW_CC
                })
            }
        }

        fun dismissControlCenter() {
            instance?.dismissPanel()
        }

        /** For the Tier 2 top strip: the whole strip is the Control Center here. */
        fun showTier2Strip(context: Context) = startControlCenter(context)

        fun isRunning(): Boolean = instance != null

        const val ACTION_SHOW_CC = "com.springboard.launcher.action.SHOW_CC"
        const val ACTION_HIDE = "com.springboard.launcher.action.HIDE"
        const val ACTION_GUARD = "com.springboard.launcher.action.GUARD"
    }
}