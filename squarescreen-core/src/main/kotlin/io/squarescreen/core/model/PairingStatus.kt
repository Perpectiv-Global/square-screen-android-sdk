package io.squarescreen.core.model

/**
 * Represents every possible outcome of the device pairing flow.
 *
 * Emitted by [io.squarescreen.player.SquareScreenPairing.pairingStatus] and returned by
 * [io.squarescreen.player.SquareScreenPairing.checkPairStatus].
 */
sealed class PairingStatus {

    /** Admin approval is still pending. The SDK continues polling automatically. */
    data object Pending : PairingStatus()

    /**
     * Admin approved this device. Store [deviceId] and [deviceToken] securely —
     * the token is returned exactly once and will not be shown again.
     *
     * Recommended storage: `EncryptedSharedPreferences`.
     */
    data class Approved(
        val deviceId: String,
        val deviceToken: String
    ) : PairingStatus()

    /**
     * The pairing token was rejected by the server (invalid or already consumed).
     * Restart the pairing flow from the beginning.
     */
    data object InvalidToken : PairingStatus()

    /**
     * The 10-minute pairing window expired before an admin approved the device.
     * Restart the pairing flow from the beginning.
     */
    data object Expired : PairingStatus()

    /**
     * No device with the supplied OS identifier was pre-registered by an admin.
     * Contact your SquareScreen workspace admin to add this device first.
     */
    data object DeviceNotFound : PairingStatus()

    /**
     * This device is already paired. Credentials should be in local secure storage.
     * Call [io.squarescreen.player.SquareScreen.init] directly with those credentials.
     */
    data object AlreadyPaired : PairingStatus()

    /** An unexpected error occurred. Inspect [throwable] for details. */
    data class Error(val throwable: Throwable) : PairingStatus()
}
