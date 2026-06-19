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
 * On first collection of [pairingStatus]:
 * - If a pairing token is stored from a previous session, skips register and goes
 *   straight to polling `pair-status`.
 * - Otherwise calls `register`, persists the returned token, then polls `pair-status`.
 *
 * The pairing token is cleared automatically on [PairingStatus.Approved],
 * [PairingStatus.Expired], and [PairingStatus.InvalidToken].
 *
 * Usage:
 * ```kotlin
 * val pairing = SquareScreenPairing.create(context, osIdentifier = androidId)
 *
 * pairing.pairingStatus.collect { status ->
 *     when (status) {
 *         is PairingStatus.Approved -> {
 *             credentialStore.save(status.deviceId, status.deviceToken)
 *             initializeSdk(status.deviceId, status.deviceToken)
 *         }
 *         PairingStatus.Pending        -> showAwaitingApprovalUI()
 *         PairingStatus.DeviceNotFound -> showDeviceNotFoundError()
 *         PairingStatus.AlreadyPaired  -> showAlreadyPairedError()
 *         PairingStatus.Expired        -> showExpiredError()
 *         PairingStatus.InvalidToken   -> showInvalidTokenError()
 *         is PairingStatus.Error       -> showGenericError(status.throwable)
 *     }
 * }
 *
 * val status = pairing.checkPairStatus() // manual one-shot check
 * pairing.cancel()                       // call in onDestroy / DisposableEffect
 * ```
 *
 * @param osIdentifier A stable device identifier pre-registered by an admin (e.g. Android ID).
 */
class SquareScreenPairing private constructor(
    private val osIdentifier: String,
    private val networkClient: PairingNetworkClient,
    private val tokenStore: PairingTokenStore,
    private val logger: SquareScreenLogger? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var currentPairingToken: String? = null

    /**
     * Flow of [PairingStatus] updates.
     *
     * On first collection:
     * - Stored token found → skips register, polls pair-status immediately.
     * - No stored token → calls register, stores token, then polls pair-status.
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
            logger?.debug(TAG, "Registering device with identifier: $osIdentifier")
            when (val reg = networkClient.register(osIdentifier)) {
                is SquareScreenResult.Error -> {
                    val status = reg.error.toPairingStatus()
                    logger?.debug(TAG, "Register failed: $status")
                    send(status)
                    close()
                    return@callbackFlow
                }
                is SquareScreenResult.Success -> {
                    currentPairingToken = reg.data.pairingToken
                    tokenStore.save(reg.data.pairingToken)
                    logger?.debug(TAG, "Registered — token stored (expires in ${reg.data.expiresIn}s)")
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
     * Only valid after [pairingStatus] has been collected and the register call has completed.
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
         * Creates a [SquareScreenPairing] instance.
         *
         * @param context Application or Activity context used for persisting the pairing token.
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
                osIdentifier = osIdentifier,
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
