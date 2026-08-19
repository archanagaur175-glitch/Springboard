package com.springboard.launcher.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.data.db.GridItemEntity
import com.springboard.launcher.data.db.GridItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the "pager overlay blocks icon taps" regression at the GridPage level: a rendered app
 * icon (label text inside the icon cell) must still reach the onTapApp callback when tapped.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class GridPageTapTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tapOnAppIconInvokesCallback() {
        val app = InstalledApp(
            packageName = "com.example.testapp",
            label = "TestApp",
            iconVersion = 1L,
        )
        val item = GridItemEntity(
            id = 1,
            type = GridItemType.APP,
            refKey = app.packageName,
            page = 0,
            slot = 0,
        )
        var tapped: String? = null

        composeRule.setContent {
            GridPage(
                page = 0,
                items = listOf(item),
                installed = mapOf(app.packageName to app),
                folders = emptyMap(),
                folderNames = emptyMap(),
                appRepository = AppRepository(
                    RuntimeEnvironment.getApplication(),
                    56,
                    CoroutineScope(SupervisorJob() + Dispatchers.Default),
                    {},
                ),
                jiggle = false,
                dragController = DragController(),
                onTapApp = { tapped = it },
                onTapFolder = {},
                onLongPressItem = {},
                onRemoveItem = {},
                onSwapItems = { _, _ -> },
                onMoveToSlot = { _, _ -> },
                onDropInFolder = { _, _ -> },
                onDropOnDock = {},
                modifier = Modifier.padding(24.dp),
            )
        }

        composeRule.onNodeWithText("TestApp").performTouchInput { click() }

        assertEquals("com.example.testapp", tapped)
    }
}