package com.springboard.launcher.ui.designsystem

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the "WallpaperBackground renders nothing" regression: the composable must emit a node
 * and actually paint an opaque gradient, not throw away its drawBehind modifier.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class WallpaperBackgroundTest {

    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun wallpaperActuallyPaintsOpaquePixels() {
        composeRule.setContent {
            WallpaperBackground(index = 0, modifier = Modifier.fillMaxSize())
        }

        val pixels = composeRule.onRoot().captureToImage().toPixelMap()

        val cx = pixels.width / 2
        val cy = pixels.height / 2
        val center = pixels[cx, cy]

        val alpha = (center.alpha * 255f).roundToInt()
        assertEquals("center pixel should be opaque, got alpha=$alpha", 255, alpha)
    }
}