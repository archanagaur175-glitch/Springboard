package com.springboard.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.data.system.SystemStateRepository
import com.springboard.launcher.ui.designsystem.GlassSurface

/**
 * The recents switcher, fed by the launch-history flow. Recently opened apps appear
 * as glass cards; tapping one relaunches it and swipe-up (or tapping the background
 * or header) dismisses the switcher.
 */
@Composable
fun RecentsSwitcher(
    onClose: () -> Unit,
    recentPackages: List<String>,
    appRepository: AppRepository,
    systemState: SystemStateRepository,
    onLaunch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps = recentPackages.mapNotNull { appRepository.appFor(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                    },
                    onDragEnd = { onClose() },
                )
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent apps",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Close",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onClose() })
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(30.dp))

            if (apps.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp)) {
                    Text(
                        text = "No recent apps yet",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        RecentsCard(
                            app = app,
                            appRepository = appRepository,
                            onLaunch = {
                                onLaunch(app.packageName)
                                onClose()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
            Text(
                text = "Swipe up to close",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecentsCard(
    app: InstalledApp,
    appRepository: AppRepository,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .width(150.dp)
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(22.dp))
            .pointerInput(app.packageName) {
                detectTapGestures(onTap = { onLaunch() })
            },
        blurRadius = 36f,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIconView(
                        app = app,
                        appRepository = appRepository,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}