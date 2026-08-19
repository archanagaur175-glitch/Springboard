package com.springboard.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.springboard.launcher.systemui.StatusBarController
import com.springboard.launcher.ui.designsystem.SpringboardTheme
import com.springboard.launcher.ui.home.LauncherScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpringboardTheme {
                LauncherScreen(application as SpringboardApp)
            }
        }
        StatusBarController.hideBars(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) StatusBarController.hideBars(this)
    }
}