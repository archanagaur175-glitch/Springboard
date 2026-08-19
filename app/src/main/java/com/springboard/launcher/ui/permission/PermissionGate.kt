package com.springboard.launcher.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.ui.designsystem.GlassSurface
import com.springboard.launcher.ui.designsystem.IosBlue

/**
 * The contextual permission gates Springboard needs for its simulated system surfaces.
 * Each kind explains why the permission matters and opens exactly the right settings
 * screen; dismissing keeps everything in-app and nothing is enforced.
 */
enum class GateKind {
    OVERLAY,
    WRITE_SETTINGS,
    NOTIFICATION_LISTENER,
    NOTIFICATION_ACCESS,
    NONE,
}

@Composable
fun PermissionGate(
    kind: GateKind,
    onOpenSettings: (Intent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val text = when (kind) {
        GateKind.OVERLAY -> Pair(
            "Control Center needs Overlay access",
            "Springboard draws the simulated Control Center as a system overlay so it can " +
                "appear above other apps (Tier 2) and over the lock screen. It is only shown " +
                "when you open it.",
        )
        GateKind.WRITE_SETTINGS -> Pair(
            "Brightness control needs Settings access",
            "Adjusting the real screen brightness requires Settings access. Without it, " +
                "brightness stays at the system value.",
        )
        GateKind.NOTIFICATION_LISTENER -> Pair(
            "Notifications need listener access",
            "Springboard renders the Notification Center itself, so the system must be told " +
                "this app is a notification listener. Notifications are never shared anywhere.",
        )
        GateKind.NOTIFICATION_ACCESS -> Pair(
            "Show notifications",
            "Springboard needs notification permission to surface your notifications in the " +
                "Notification Center and to keep the always-on clock alive.",
        )
        GateKind.NONE -> Pair("", "")
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF20F0F0F))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { /* swallow taps inside the card */ })
                }
                .padding(24.dp),
            blurRadius = 36f,
        ) {
            Column {
                Text(
                    text = text.first,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = text.second,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row {
                    Text(
                        text = "Not now",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onDismiss() })
                            }
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Open Settings",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(IosBlue)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    onOpenSettings(intentFor(kind, context.packageName))
                                })
                            }
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun intentFor(kind: GateKind, packageName: String): Intent = when (kind) {
    GateKind.OVERLAY -> Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
    )
    GateKind.WRITE_SETTINGS -> Intent(
        Settings.ACTION_MANAGE_WRITE_SETTINGS,
        Uri.parse("package:$packageName"),
    )
    GateKind.NOTIFICATION_LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    GateKind.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    GateKind.NONE -> Intent(Settings.ACTION_SETTINGS)
}