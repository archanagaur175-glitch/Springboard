package com.springboard.launcher.ui.onboarding

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.springboard.launcher.data.prefs.AppPrefs
import com.springboard.launcher.ui.designsystem.IosBlue
import com.springboard.launcher.ui.designsystem.SpringboardTheme
import com.springboard.launcher.ui.designsystem.WallpaperCatalog
import kotlinx.coroutines.launch

/**
 * First-run onboarding, launched automatically from MainActivity until the user finishes it.
 * Walks through becoming the default home app, granting overlay access, enabling the lock
 * facade and picking a wallpaper. Nothing is mandatory; Finish marks onboarding complete.
 */
class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SpringboardApp
        setContent {
            SpringboardTheme {
                OnboardingScreen(
                    app = app,
                    onDone = { finish() },
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    app: SpringboardApp,
    onDone: () -> Unit,
) {
    val container = app.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wallpaper by remember { mutableIntStateOf(AppPrefs.wallpaperIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(34.dp))
        Text(
            text = "Welcome to Springboard",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "A few quick choices, then you're home.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(26.dp))

        StepCard(
            number = 1,
            title = "Make Springboard your home app",
            subtitle = "Set it as the default launcher so the Home button brings you here.",
            actionLabel = "Set as home",
            action = { requestHomeRole(context) },
        )
        Spacer(Modifier.height(14.dp))

        StepCard(
            number = 2,
            title = "Allow overlay drawing",
            subtitle = "Lets the simulated Control Center appear over other apps and the lock screen.",
            actionLabel = "Open overlay settings",
            action = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            },
        )
        Spacer(Modifier.height(14.dp))

        StepCard(
            number = 3,
            title = "Enable the lock facade",
            subtitle = "Shows the Springboard clock and notifications over the keyguard on screen-on.",
            actionLabel = "Enable",
            action = { scope.launch { container.settings.setLockFacadeEnabled(true) } },
        )
        Spacer(Modifier.height(14.dp))

        StepCard(
            number = 4,
            title = "Pick a wallpaper",
            subtitle = "Tap a gradient to preview it; it applies to the whole home screen.",
            actionLabel = null,
            action = null,
        ) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WallpaperCatalog.wallpapers.forEachIndexed { index, spec ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WallpaperCatalog.brushFor(index))
                            .then(
                                if (index == wallpaper) {
                                    Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .pointerInput(index) {
                                detectTapGestures(onTap = {
                                    wallpaper = index
                                    scope.launch { container.settings.setWallpaperIndex(index) }
                                })
                            },
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))

        Text(
            text = "You can change all of this later from the gear on your home screen.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Finish",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(IosBlue)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        scope.launch { container.settings.setOnboardingComplete(true) }
                        onDone()
                    })
                }
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    subtitle: String,
    actionLabel: String?,
    action: (() -> Unit)?,
    content: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1C1C1E))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(IosBlue)
                    .padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (content != null) {
            content()
        }
        if (actionLabel != null && action != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = actionLabel,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .pointerInput(actionLabel) {
                        detectTapGestures(onTap = { action() })
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

private fun requestHomeRole(context: android.content.Context) {
    val roleManager = context.getSystemService(RoleManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
        runCatching {
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                context.startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            } else {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        }
    } else {
        runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
    }
}