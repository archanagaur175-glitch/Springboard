package com.springboard.launcher.systemui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.springboard.launcher.data.prefs.AppPrefs
import com.springboard.launcher.ui.lockscreen.LockScreenActivity

/**
 * Screen-on receiver that surfaces the Springboard lock facade. The activity uses
 * setShowWhenLocked, so it renders over the device keyguard. It never authenticates;
 * swipe-up finishes and hands off to the real unlock flow. If this fires while the user
 * already unlocked (race with a quick unlock), it does nothing.
 */
class LockFacadeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_ON) return
        if (!AppPrefs.lockFacadeEnabled) return

        val keyguard = runCatching {
            context.getSystemService(KeyguardManager::class.java)
        }.getOrNull()
        if (keyguard == null || !keyguard.isKeyguardLocked) return

        val launch = Intent(context, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        try {
            context.startActivity(launch)
        } catch (t: Throwable) {
            // Background-start denied (no overlay permission yet): the real keyguard
            // shows instead, which is the documented, acceptable fallback.
            Log.w(TAG, "Lock facade start blocked", t)
        }
    }

    private companion object {
        const val TAG = "LockFacadeReceiver"
    }
}