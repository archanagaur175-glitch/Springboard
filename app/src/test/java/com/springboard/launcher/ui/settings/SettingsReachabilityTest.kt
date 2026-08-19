package com.springboard.launcher.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.core.app.ApplicationProvider
import com.springboard.launcher.SpringboardApp
import com.springboard.launcher.ui.home.LauncherScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the "SettingsActivity is unreachable" regression: the home screen exposes a discoverable
 * gear affordance, and activating it must launch SettingsActivity.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SettingsReachabilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsGearLaunchesSettingsActivity() {
        val app = ApplicationProvider.getApplicationContext<SpringboardApp>()
        composeRule.setContent {
            LauncherScreen(app)
        }

        composeRule.onNodeWithContentDescription("Open settings").performTouchInput { click() }

        val intent = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals(
            "com.springboard.launcher.ui.settings.SettingsActivity",
            intent.component?.className,
        )
    }
}