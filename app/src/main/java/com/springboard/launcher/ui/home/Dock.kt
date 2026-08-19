package com.springboard.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.data.db.DockItemEntity
import com.springboard.launcher.ui.designsystem.GlassSurface

/** Persistent glass dock, fixed across pages, holding the pinned app set. */
@Composable
fun Dock(
    items: List<DockItemEntity>,
    installed: Map<String, InstalledApp>,
    appRepository: AppRepository,
    backdrop: Brush,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(28.dp))
            .padding(6.dp),
        backdrop = backdrop,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.sortedBy { it.slot }.take(6).forEach { item ->
                val app = installed[item.packageName]
                if (app != null) {
                    AppIconView(
                        app = app,
                        appRepository = appRepository,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
            }
        }
    }
}