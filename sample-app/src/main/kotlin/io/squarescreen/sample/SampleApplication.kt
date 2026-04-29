package io.squarescreen.sample

import android.app.Application
import io.squarescreen.core.config.ForegroundNotificationConfig
import io.squarescreen.core.config.SquareScreenConfig
import io.squarescreen.core.logging.SquareScreenDebugLogger
import io.squarescreen.player.SquareScreen

/**
 * Application entry point. SDK initialization happens here, in onCreate(), before any
 * Activity or Service starts. This is the only correct place to call SquareScreen.init().
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeSquareScreen()
    }

    private fun initializeSquareScreen() {
        // --- Device credentials ---
        //
        // In a production app, NEVER hardcode the deviceToken here.
        // The recommended flow is:
        //   1. On first launch, call your backend with a device identifier
        //      (e.g. Settings.Secure.ANDROID_ID or a generated UUID).
        //   2. Your backend provisions the device and returns a token.
        //   3. Store the token in EncryptedSharedPreferences.
        //   4. Read it here at runtime.
        //
        // This sample uses hardcoded placeholder values for demonstration only.
        val deviceId = "device-sample-uuid-0001"
        val deviceToken = "sample-device-token-replace-me"

        SquareScreen.init(
            context = this,
            config = SquareScreenConfig(
                // Replace with your SquareScreen API base URL.
                baseUrl = "https://api.squarescreen.io",
                deviceId = deviceId,
                deviceToken = deviceToken,

                // Heartbeat interval: how often the SDK reports device health metrics
                // (CPU, memory, disk, temperature) to the server. Minimum 30s.
                heartbeatIntervalSeconds = 60L,

                // Emergency poll interval: how often the SDK checks for active emergency
                // broadcasts. Keep this low — a 30s delay on a fire alarm matters.
                emergencyPollIntervalSeconds = 30L,

                // Cache TTL: how long a fetched playlist is considered fresh before
                // the SDK re-fetches from the network. 1 hour is a sensible default
                // for digital signage where content changes infrequently.
                cacheTtlSeconds = 3600L,

                // The foreground service notification keeps this app alive as a display
                // player. Android requires a visible notification for foreground services.
                foregroundNotification = ForegroundNotificationConfig(
                    title = "SquareScreen Player",
                    iconResId = R.drawable.ic_player
                ),

                // Logger: only log in debug builds. Production builds are completely
                // silent — the SDK never spams Logcat without an explicit logger.
                logger = if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null
            )
        )
    }
}
