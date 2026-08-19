package com.springboard.launcher.ui.lockscreen

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.systemui.StatusBarController
import com.springboard.launcher.ui.designsystem.SpringboardTheme

/**
 * The Springboard lock facade. Rendered over the device keyguard via showWhenLocked;
 * it never authenticates. Swipe-up finishes the activity and hands off to the real
 * unlock flow.
 */
class LockScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        StatusBarController.hideBars(this)
        setContent {
            SpringboardTheme {
                LockScreen(
                    app = application as SpringboardApp,
                    onUnlock = { finishAndRemoveTask() },
                )
            }
        }
    }
}