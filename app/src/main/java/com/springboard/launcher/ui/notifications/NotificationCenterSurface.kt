package com.springboard.launcher.ui.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.system.SystemStateRepository

/** Placeholder; the full Notification Center surface lands with the lock facade feature. */
@Composable
fun NotificationCenterSurface(
    onClose: () -> Unit,
    systemState: SystemStateRepository,
    appRepository: AppRepository,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Notification Center", color = Color.White)
    }
}