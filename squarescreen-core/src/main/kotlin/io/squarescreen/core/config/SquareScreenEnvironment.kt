package io.squarescreen.core.config

/**
 * Selects which SquareScreen API environment the SDK connects to.
 *
 * Pass via [SquareScreenConfig.environment] on init, and via the `environment`
 * parameter on [io.squarescreen.player.SquareScreenPairing.create] /
 * [io.squarescreen.player.SquareScreenPairing.createWithActivation].
 */
enum class SquareScreenEnvironment(val baseUrl: String) {
    LIVE("https://api.squarescreen.io/api/v1"),
    TEST("https://testapi.squarescreen.io/api/v1")
}
