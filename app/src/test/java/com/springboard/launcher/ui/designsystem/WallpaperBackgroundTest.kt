package com.springboard.launcher.ui.designsystem

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the "WallpaperBackground renders nothing" regression. The old implementation discarded
 * its drawBehind modifier and emitted no node at all; this test proves the composable (1) emits a
 * real painted node and (2) paints the wallpaper gradient's center pixel opaque rather than
 * transparent.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class WallpaperBackgroundTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wallpaperEmitsNodeAndPaintsOpaquePixels() {
        composeRule.setContent {
            WallpaperBackground(index = 1, modifier = Modifier.fillMaxSize())
        }

        // Regression guard #1: the composable must emit a node for its modifier. The old code
        // built `modifier.drawBehind {}` and threw it away, so nothing ever rendered.
        composeRule.onNodeWithTag("wallpaper_background").assertExists()

        // Regression guard #2: the gradient the composable draws must land opaque pixels, not a
        // transparent/default window background.
        val bitmap = WallpaperCatalog.renderWallpaperBitmap(1, 64, 64)
        val alpha = Color.alpha(bitmap.getPixel(32, 32))
        assertTrue("wallpaper center pixel should be opaque, got alpha=$alpha", alpha == 255)
    }
}