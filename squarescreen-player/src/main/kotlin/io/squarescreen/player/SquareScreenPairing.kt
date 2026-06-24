package io.squarescreen.player

import android.content.Context
import io.squarescreen.core.logging.SquareScreenLogger
import io.squarescreen.core.model.PairingStatus
import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.network.PairingNetworkClient
import io.squarescreen.player.internal.PairingTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

private const val TAG = "SquareScreenPairing"

/**
 * Manages the device pairing flow before [SquareScreen.init] can be called.
 *
 * Two factory methods cover the two registration paths:
 * - [create] — register path: SDK sends an OS identifier; admin confirms in the dashboard.
 * - [createWithActivation] — activate path: developer supplies a SquareScreen device ID
 *   (8-character alphanumeric) and their own device token (IMEI, UUID, etc.).
 *
 * Both paths converge at the same `pair-status` polling loop and emit the same
 * [PairingStatus] states.
 *
 * If a pairing token from a previous session is stored, both paths skip their
 * registration call and go straight to polling.
 *
 * Usage (register path):
 * ```kotlin
 * val pairing = SquareScreenPairing.create(context, osIdentifier = androidId)
 * pairing.pairingStatus.collect { status -> ... }
 * ```
 *
 * Usage (activate path):
 * ```kotlin
 * val pairing = SquareScreenPairing.createWithActivation(
 *     context = context,
 *     deviceId = "AB12CD34",
 *     deviceToken = telephonyManager.imei ?: uuid
 * )
 * pairing.pairingStatus.collect { status -> ... }
 * ```
 */
class SquareScreenPairing private constructor(
    private val registrationMode: RegistrationMode,
    private val networkClient: PairingNetworkClient,
    private val tokenStore: PairingTokenStore,
    private val logger: SquareScreenLogger? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var currentPairingToken: String? = null

    internal sealed class RegistrationMode {
        data class Register(val osIdentifier: String) : RegistrationMode()
        data class Activate(val deviceId: String, val deviceToken: String) : RegistrationMode()
    }

    /**
     * Flow of [PairingStatus] updates.
     *
     * On first collection:
     * - Stored token found → skips registration, polls pair-status immediately.
     * - No stored token → calls register or activate (depending on factory used),
     *   stores the returned token, then polls pair-status.
     *
     * Polls with exponential backoff (5 s × 3 → 10 s × 3 → 30 s thereafter).
     * Terminates automatically on any terminal [PairingStatus].
     * The last emitted value is replayed to new collectors.
     */
    val pairingStatus: Flow<PairingStatus> = callbackFlow<PairingStatus> {
        val storedToken = tokenStore.get()

        if (storedToken != null) {
            logger?.debug(TAG, "Resuming pairing with stored token")
            currentPairingToken = storedToken
            send(PairingStatus.Pending)
        } else {
            val result = when (val mode = registrationMode) {
                is RegistrationMode.Register -> {
                    logger?.debug(TAG, "Registering device: ${mode.osIdentifier}")
                    networkClient.register(mode.osIdentifier)
                }
                is RegistrationMode.Activate -> {
                    logger?.debug(TAG, "Activating device: ${mode.deviceId}")
                    networkClient.activate(mode.deviceId, mode.deviceToken)
                }
            }

            when (result) {
                is SquareScreenResult.Error -> {
                    val status = result.error.toPairingStatus()
                    logger?.debug(TAG, "Registration failed: $status")
                    send(status)
                    close()
                    return@callbackFlow
                }
                is SquareScreenResult.Success -> {
                    currentPairingToken = result.data.pairingToken
                    tokenStore.save(result.data.pairingToken)
                    logger?.debug(TAG, "Token stored (expires in ${result.data.expiresIn}s)")
                    send(PairingStatus.Pending)
                }
            }
        }

        var attempt = 0
        while (true) {
            delay(backoffDelay(attempt))
            attempt++
            val token = currentPairingToken ?: break
            logger?.debug(TAG, "Polling pair status (attempt $attempt)")
            val status = networkClient.getPairStatus(token)
            send(status)
            when (status) {
                is PairingStatus.Approved,
                PairingStatus.InvalidToken,
                PairingStatus.Expired -> {
                    logger?.debug(TAG, "Pairing terminal: $status — clearing stored token")
                    tokenStore.clear()
                    close()
                    return@callbackFlow
                }
                is PairingStatus.Error -> {
                    logger?.debug(TAG, "Pairing error: ${status.throwable.message}")
                    close()
                    return@callbackFlow
                }
                else -> {}
            }
        }

        awaitClose()
    }.shareIn(scope, SharingStarted.Lazily, replay = 1)

    /**
     * Performs a one-shot pair-status check outside the automatic polling cycle.
     *
     * Only valid after [pairingStatus] has been collected and registration has completed.
     * Returns [PairingStatus.Error] if there is no active pairing token.
     */
    suspend fun checkPairStatus(): PairingStatus {
        val token = currentPairingToken
            ?: return PairingStatus.Error(
                IllegalStateException("No active pairing session — collect pairingStatus first")
            )
        return networkClient.getPairStatus(token)
    }

    /**
     * Cancels the polling coroutine scope. Call this when the pairing screen is destroyed.
     */
    fun cancel() {
        scope.cancel()
    }

    // Backoff: attempts 0–2 → 5s, 3–5 → 10s, 6+ → 30s
    private fun backoffDelay(attempt: Int): Long = when {
        attempt < 3 -> 5_000L
        attempt < 6 -> 10_000L
        else -> 30_000L
    }

    companion object {
        /**
         * Register path: the SDK sends [osIdentifier] to the server and waits for an
         * admin to confirm the device in the SquareScreen dashboard.
         *
         * @param context Application or Activity context (used for token persistence).
         * @param osIdentifier Stable device identifier pre-registered by an admin.
         *   On Android, use `Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)`.
         * @param logger Optional logger for debug output. Null (default) = silent.
         */
        fun create(
            context: Context,
            osIdentifier: String,
            logger: SquareScreenLogger? = null
        ): SquareScreenPairing {
            require(osIdentifier.isNotBlank()) { "osIdentifier must not be blank" }
            return SquareScreenPairing(
                registrationMode = RegistrationMode.Register(osIdentifier),
                networkClient = PairingNetworkClient(),
                tokenStore = PairingTokenStore(context.applicationContext),
                logger = logger
            )
        }

        /**
         * Activate path: the developer supplies a SquareScreen [deviceId] (8-character
         * alphanumeric string issued by SquareScreen) and their own [deviceToken]
         * (IMEI, UUID, or any stable identifier they choose).
         *
         * The flow then polls `pair-status` identically to the register path.
         *
         * @param context Application or Activity context (used for token persistence).
         * @param deviceId 8-character alphanumeric SquareScreen device ID.
         * @param deviceToken Developer-chosen device token (e.g. IMEI, installation UUID).
         * @param logger Optional logger for debug output. Null (default) = silent.
         */
        fun createWithActivation(
            context: Context,
            deviceId: String,
            deviceToken: String,
            logger: SquareScreenLogger? = null
        ): SquareScreenPairing {
            require(deviceId.isNotBlank()) { "deviceId must not be blank" }
            require(deviceToken.isNotBlank()) { "deviceToken must not be blank" }
            return SquareScreenPairing(
                registrationMode = RegistrationMode.Activate(deviceId, deviceToken),
                networkClient = PairingNetworkClient(),
                tokenStore = PairingTokenStore(context.applicationContext),
                logger = logger
            )
        }
    }
}

private fun SquareScreenError.toPairingStatus(): PairingStatus = when (this) {
    is SquareScreenError.NetworkError -> when (code) {
        404 -> PairingStatus.DeviceNotFound
        409 -> PairingStatus.AlreadyPaired
        else -> PairingStatus.Error(Exception("Network error $code: $message"))
    }
    is SquareScreenError.Unknown -> PairingStatus.Error(throwable)
    is SquareScreenError.ParseError -> PairingStatus.Error(Exception(message))
    else -> PairingStatus.Error(Exception(toString()))
}
