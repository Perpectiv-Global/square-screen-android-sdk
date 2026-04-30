package io.squarescreen.sample

import android.app.Application
import io.squarescreen.core.config.ForegroundNotificationConfig
import io.squarescreen.core.config.SquareScreenConfig
import io.squarescreen.core.logging.SquareScreenDebugLogger
import io.squarescreen.player.SquareScreen
import io.squarescreen.sample.data.CredentialStore
import io.squarescreen.sample.data.DeviceCredentials

/**
 * Application entry point.
 *
 * SDK initialization is conditional — it only happens if this device has already
 * been paired (i.e. credentials are saved in EncryptedSharedPreferences).
 *
 * First launch: credentials are absent → SDK is NOT initialized here.
 *   The user sees the PairingScreen in MainActivity, which handles registration
 *   and calls initializeSdk() once credentials are obtained.
 *
 * Subsequent launches: credentials are present → SDK is initialized here, before
 *   any Activity starts, so the player is ready immediately.
 */
class SampleApplication : Application() {

    lateinit var credentialStore: CredentialStore
        private set

    override fun onCreate() {
        super.onCreate()
        credentialStore = CredentialStore(this)
        credentialStore.saveCredentials(DeviceCredentials("a", "a"))

        // Initialize the SDK only if we already have credentials.
        // If this is the first launch, MainActivity will call initializeSdk()
        // after the device is successfully registered.
        credentialStore.getCredentials()?.let { credentials ->
            initializeSdk(credentials)
        }
    }

    /**
     * Initializes the SquareScreen SDK with the given device credentials.
     *
     * This is safe to call from any thread — SDK init is synchronized internally.
     * Calling it more than once is a no-op (the SDK logs a warning and returns).
     */
    fun initializeSdk(credentials: DeviceCredentials) {
        SquareScreen.init(
            context = this,
            config = SquareScreenConfig(
                deviceId = credentials.deviceId,
                deviceToken = credentials.deviceToken,

                // Heartbeat: reports device health (CPU, memory, disk, temperature) every 60s.
                heartbeatIntervalSeconds = 60L,

                // Emergency poll: checks for active broadcasts every 30s.
                // Keep this short — delays on a fire alarm broadcast matter.
                emergencyPollIntervalSeconds = 30L,

                // Cache TTL: cached playlists are considered fresh for 1 hour.
                cacheTtlSeconds = 3600L,

                foregroundNotification = ForegroundNotificationConfig(
                    title = "SquareScreen Player",
                    iconResId = R.drawable.ic_player
                ),

                // Only log in debug builds. Production is completely silent.
                logger = if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null
            )
        )
    }
}
