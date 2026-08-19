package com.springboard.launcher.ui.applibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.domain.AppCategory
import com.springboard.launcher.domain.SearchScorer
import com.springboard.launcher.ui.home.AppIconView
import com.springboard.launcher.ui.designsystem.rememberSquircleShape

/**
 * The App Library page: a Spotlight-style search field with live fuzzy scoring on top,
 * and a category-grouped grid of every installed app when the query is empty.
 */
@Composable
fun AppLibraryPage(
    apps: List<InstalledApp>,
    appRepository: AppRepository,
    onTapApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    val searched = remember(query, apps) {
        if (query.isBlank()) apps else SearchScorer.rank(apps, query)
    }
    val categories = remember(searched) {
        AppCategory.sortedCategoryNames(searched.map { it.label to it.packageName })
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 17.sp,
                        )
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            searched.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No results for \"$query\"",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            query.isBlank() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { category ->
                        item(key = "header-$category", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = category.uppercase(),
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp),
                            )
                        }
                        items(
                            searched.filter { AppCategory.categoryFor(it.label, it.packageName) == category },
                            key = { it.packageName },
                        ) { app ->
                            LibraryCell(app = app, appRepository = appRepository, onTapApp = onTapApp)
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                ) {
                    items(searched, key = { it.packageName }) { app ->
                        LibraryCell(app = app, appRepository = appRepository, onTapApp = onTapApp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCell(
    app: InstalledApp,
    appRepository: AppRepository,
    onTapApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = rememberSquircleShape()
    Column(
        modifier = modifier
            .padding(6.dp)
            .clip(shape)
            .pointerInput(app.packageName) {
                detectTapGestures(onTap = { onTapApp(app.packageName) })
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIconView(
            app = app,
            appRepository = appRepository,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}