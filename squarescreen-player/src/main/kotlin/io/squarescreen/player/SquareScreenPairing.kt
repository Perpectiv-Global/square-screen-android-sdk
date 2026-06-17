package io.squarescreen.player

import io.squarescreen.core.logging.SquareScreenLogger
import io.squarescreen.core.model.PairingStatus
import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.network.PairingNetworkClient
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
 * Usage:
 * ```kotlin
 * val pairing = SquareScreenPairing.create(osIdentifier = androidId)
 *
 * // Automatic — observe status changes with backoff polling
 * pairing.pairingStatus.collect { status ->
 *     when (status) {
 *         is PairingStatus.Approved -> {
 *             credentialStore.save(status.deviceId, status.deviceToken)
 *             initializeSdk(status.deviceId, status.deviceToken)
 *         }
 *         PairingStatus.Pending      -> showAwaitingApprovalUI()
 *         PairingStatus.DeviceNotFound -> showDeviceNotFoundError()
 *         PairingStatus.AlreadyPaired  -> showAlreadyPairedError()
 *         PairingStatus.Expired      -> showExpiredError()
 *         PairingStatus.InvalidToken -> showInvalidTokenError()
 *         is PairingStatus.Error     -> showGenericError(status.throwable)
 *     }
 * }
 *
 * // Manual — trigger an immediate check (e.g. from a "Check now" button)
 * val status = pairing.checkPairStatus()
 *
 * // Cancel when done (e.g. in onDestroy or DisposableEffect)
 * pairing.cancel()
 * ```
 *
 * @param osIdentifier A stable device identifier pre-registered by an admin (e.g. Android ID).
 */
class SquareScreenPairing private constructor(
    private val osIdentifier: String,
    private val networkClient: PairingNetworkClient,
    private val logger: SquareScreenLogger? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var currentPairingToken: String? = null

    /**
     * Flow of [PairingStatus] updates.
     *
     * On first collection: calls `POST /screen/register`, then polls `GET /screen/pair-status`
     * with exponential backoff (5 s × 3 → 10 s × 3 → 30 s thereafter).
     * Terminates automatically on [PairingStatus.Approved], [PairingStatus.InvalidToken],
     * [PairingStatus.Expired], [PairingStatus.DeviceNotFound], [PairingStatus.AlreadyPaired],
     * or [PairingStatus.Error].
     *
     * The last emitted value is replayed to new collectors via [shareIn].
     */
    val pairingStatus: Flow<PairingStatus> = callbackFlow<PairingStatus> {
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
                logger?.debug(
                    TAG,
                    "Registered — awaiting admin approval (token expires in ${reg.data.expiresIn}s)"
                )
                send(PairingStatus.Pending)
            }
        }

        var attempt = 0
        while (true) {
            delay(backoffDelay(attempt))
            attempt++
            val token = currentPairingToken ?: break
            logger?.debug(TAG, "Polling pair status (attempt $attempt, delay ${backoffDelay(attempt - 1)}ms)")
            val status = networkClient.getPairStatus(token)
            send(status)
            when (status) {
                is PairingStatus.Approved,
                PairingStatus.InvalidToken,
                PairingStatus.Expired,
                is PairingStatus.Error -> {
                    logger?.debug(TAG, "Pairing terminal: $status")
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
     * Returns [PairingStatus.Error] if called before an active pairing session exists.
     */
    suspend fun checkPairStatus(): PairingStatus {
        val token = currentPairingToken
            ?: return PairingStatus.Error(
                IllegalStateException("No active pairing session — collect pairingStatus first")
            )
        return networkClient.getPairStatus(token)
    }

    /**
     * Cancels the polling coroutine scope. Call this when the pairing screen is destroyed
     * to avoid leaking the polling loop.
     */
    fun cancel() {
        scope.cancel()
    }

    // Backoff schedule: attempts 0-2 → 5s, attempts 3-5 → 10s, attempts 6+ → 30s
    private fun backoffDelay(attempt: Int): Long = when {
        attempt < 3 -> 5_000L
        attempt < 6 -> 10_000L
        else -> 30_000L
    }

    companion object {
        /**
         * Creates a [SquareScreenPairing] instance for the given OS identifier.
         *
         * @param osIdentifier Stable device identifier pre-registered by an admin.
         *   On Android, use `Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)`.
         * @param logger Optional logger for debug output. Pass null (default) for silence.
         */
        fun create(
            osIdentifier: String,
            logger: SquareScreenLogger? = null
        ): SquareScreenPairing {
            require(osIdentifier.isNotBlank()) { "osIdentifier must not be blank" }
            return SquareScreenPairing(osIdentifier, PairingNetworkClient(), logger)
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
