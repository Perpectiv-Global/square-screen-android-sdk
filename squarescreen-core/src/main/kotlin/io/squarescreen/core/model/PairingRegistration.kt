package io.squarescreen.core.model

/**
 * Returned by the register endpoint after a device successfully identifies itself.
 * Holds the short-lived pairing token used to poll for admin approval.
 *
 * Not intended for direct use by integrators — [io.squarescreen.player.SquareScreenPairing]
 * uses this internally to drive the polling loop.
 *
 * @param pairingToken Short-lived token for polling [/screen/pair-status].
 * @param expiresIn Seconds until this token expires (typically 600 — 10 minutes).
 */
data class PairingRegistration(
    val pairingToken: String,
    val expiresIn: Int
)
