package io.squarescreen.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.squarescreen.player.SquareScreen
import io.squarescreen.sample.data.DeviceCredentials
import io.squarescreen.sample.ui.PairingScreen
import io.squarescreen.sample.ui.PlayerScreen
import io.squarescreen.sample.ui.theme.SquareScreenSampleTheme

/**
 * Single-activity host. Handles two top-level states:
 *
 * 1. Not paired → shows [PairingScreen] so the device can be registered.
 * 2. Paired → shows [PlayerScreen] with the active playlist.
 *
 * No navigation library is used intentionally — this is a kiosk app with
 * exactly two screens and a one-way transition between them.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SampleApplication

        setContent {
            SquareScreenSampleTheme {
                // Track whether this device has been paired.
                // Initial value comes from CredentialStore so the correct
                // screen is shown immediately without a loading flash.
                var isPaired by remember {
                    mutableStateOf(app.credentialStore.isPaired())
                }

                if (isPaired) {
                    // Credentials are available and SDK is initialized —
                    // go straight to the player.
                    PlayerScreen(squareScreen = SquareScreen.getInstance())
                } else {
                    // No credentials yet — show the pairing screen.
                    // onPaired is called after successful registration.
                    PairingScreen(
                        onPaired = { credentials ->
                            // 1. Persist credentials securely.
                            app.credentialStore.saveCredentials(credentials)
                            // 2. Initialize the SDK now that we have a token.
                            app.initializeSdk(credentials)
                            // 3. Switch to the player screen.
                            isPaired = true
                        }
                    )
                }
            }
        }
    }
}
