package com.springboard.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.springboard.launcher.systemui.StatusBarController
import com.springboard.launcher.ui.designsystem.SpringboardTheme
import com.springboard.launcher.ui.home.LauncherScreen
import com.springboard.launcher.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
        startOnboardingIfNeeded()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) StatusBarController.hideBars(this)
    }

    private fun startOnboardingIfNeeded() {
        val app = application as SpringboardApp
        lifecycleScope.launch {
            val complete = runCatching { app.container.settings.onboardingCompleteFlow.first() }
                .getOrDefault(false)
            if (!complete) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
            }
        }
    }
}