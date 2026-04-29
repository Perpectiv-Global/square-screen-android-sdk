package io.squarescreen.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.squarescreen.player.SquareScreen
import io.squarescreen.sample.ui.PlayerScreen
import io.squarescreen.sample.ui.theme.SquareScreenSampleTheme

/**
 * Single-activity host. SquareScreen display players typically run as full-screen
 * kiosk apps with a single activity — this matches that pattern.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge so the player fills the entire screen including
        // the status bar and navigation bar — standard for signage displays.
        enableEdgeToEdge()

        // Retrieve the SDK instance initialized in SampleApplication.
        // getInstance() is safe here because init() was already called in Application.onCreate().
        val squareScreen = SquareScreen.getInstance()

        setContent {
            SquareScreenSampleTheme {
                PlayerScreen(squareScreen = squareScreen)
            }
        }
    }
}
