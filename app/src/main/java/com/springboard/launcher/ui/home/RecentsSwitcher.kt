package com.springboard.launcher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.system.SystemStateRepository

/** Placeholder; the full recents switcher lands with gestures support. */
@Composable
fun RecentsSwitcher(
    onClose: () -> Unit,
    recentPackages: List<String>,
    appRepository: AppRepository,
    systemState: SystemStateRepository,
    onLaunch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Recents", color = Color.White)
    }
}