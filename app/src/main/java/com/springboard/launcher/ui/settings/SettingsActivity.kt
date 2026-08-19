package com.springboard.launcher.ui.settings

import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.provider.Settings.ACTION_HOME_SETTINGS
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.ui.designsystem.SpringboardTheme
import com.springboard.launcher.ui.designsystem.WallpaperCatalog
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SpringboardApp
        setContent {
            SpringboardTheme {
                SettingsScreen(
                    app = app,
                    onDone = { finish() },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    app: SpringboardApp,
    onDone: () -> Unit,
) {
    val container = app.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val wallpaper by container.settings.wallpaperFlow.collectAsState(initial = 0)
    val lockFacade by container.settings.lockFacadeFlow.collectAsState(initial = false)
    val tier2Cc by container.settings.tier2CcFlow.collectAsState(initial = false)

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0D))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Back",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDone() })
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp),
        ) {
            item { SectionHeader("Wallpaper") }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(WallpaperCatalog.wallpapers) { index, spec ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(WallpaperCatalog.brushFor(index))
                                .then(
                                    if (index == wallpaper) {
                                        Modifier.border(3.dp, Color.White, RoundedCornerShape(14.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .pointerInput(index) {
                                    detectTapGestures(onTap = {
                                        scope.launch { container.settings.setWallpaperIndex(index) }
                                    })
                                },
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            Text(
                                text = spec.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(6.dp).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Features") }

            item {
                ToggleRow(
                    title = "Lock Facade",
                    subtitle = "Show the Springboard lock screen over the keyguard on screen-on",
                    checked = lockFacade,
                    onCheckedChange = { scope.launch { container.settings.setLockFacadeEnabled(it) } },
                )
            }
            item {
                ToggleRow(
                    title = "System-wide Control Center (Tier 2)",
                    subtitle = "Keep a Control Center entry point on top of other apps too. Experimental.",
                    checked = tier2Cc,
                    onCheckedChange = { scope.launch { container.settings.setTier2CcEnabled(it) } },
                )
            }

            item { SectionHeader("Permissions") }

            item {
                SettingsRow(title = "Overlay (Control Center)", onClick = {
                    context.startActivity(
                        Intent(ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                    )
                })
            }
            item {
                SettingsRow(title = "Settings access (brightness)", onClick = {
                    context.startActivity(
                        Intent(ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                })
            }
            item {
                SettingsRow(title = "Notification listener", onClick = {
                    context.startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
                })
            }
            item {
                SettingsRow(title = "App notifications", onClick = {
                    context.startActivity(
                        Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, context.packageName),
                    )
                })
            }

            item { SectionHeader("System") }

            item {
                SettingsRow(title = "Set as home app", onClick = {
                    runCatching { context.startActivity(Intent(ACTION_HOME_SETTINGS)) }
                })
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Springboard never reads or sends your data; toggles and permissions are used strictly to render its simulated surfaces on-device.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF8E8E93),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E))
            .pointerInput(title) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, color = Color.White, fontSize = 16.sp)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            TogglePill(checked = checked) { onCheckedChange(!checked) }
        }
    }
}

@Composable
private fun TogglePill(checked: Boolean, onClick: () -> Unit) {
    val bg = if (checked) Color(0xFF30D158) else Color(0xFF3A3A3C)
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(bg)
            .pointerInput(checked) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}