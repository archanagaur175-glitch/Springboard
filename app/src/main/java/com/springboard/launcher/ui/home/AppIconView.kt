package com.springboard.launcher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.ui.designsystem.rememberSquircleShape

/**
 * Renders one installed-app icon with Springboard's squircle. The bitmap is loaded once
 * per (package, iconVersion) and cached by the repository, so scrolling stays smooth.
 */
@Composable
fun AppIconView(
    app: InstalledApp,
    appRepository: AppRepository,
    modifier: Modifier = Modifier,
    iconSize: Dp = 54.dp,
) {
    val shape = rememberSquircleShape()
    val icon by produceState<ImageBitmap?>(null, app.packageName, app.iconVersion) {
        value = appRepository.loadIcon(app)
    }
    Box(
        modifier = modifier
            .size(iconSize)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = app.label.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }
    }
}