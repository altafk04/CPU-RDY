package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.ContentionSeverity
import com.example.ui.components.CpuReadyGauge
import com.example.ui.theme.AppTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GaugeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cpu_ready_gauge_screenshot() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                CpuReadyGauge(
                    readyPercent = 8.5,
                    perVcpuPercent = 4.25,
                    vCpuCount = 2,
                    severity = ContentionSeverity.NORMAL
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/cpu_gauge.png")
    }
}
