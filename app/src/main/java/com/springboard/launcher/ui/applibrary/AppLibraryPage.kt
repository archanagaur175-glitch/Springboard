package com.springboard.launcher.ui.applibrary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.ui.home.AppIconView

/** Placeholder; the full App Library (auto-categories + spotlight search) lands next. */
@Composable
fun AppLibraryPage(
    apps: List<InstalledApp>,
    appRepository: AppRepository,
    onTapApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(top = 24.dp)) {
        Text("App Library", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppIconView(app = app, appRepository = appRepository, modifier = Modifier.padding(8.dp))
            }
        }
    }
}