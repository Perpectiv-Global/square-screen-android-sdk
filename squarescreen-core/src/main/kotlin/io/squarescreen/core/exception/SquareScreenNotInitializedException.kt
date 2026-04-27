package io.squarescreen.core.exception

/**
 * Thrown when [io.squarescreen.player.SquareScreen.getInstance] is called before
 * [io.squarescreen.player.SquareScreen.init] has been invoked.
 *
 * Fix: call `SquareScreen.init(context, config)` in your `Application.onCreate()`
 * before accessing any SDK functionality.
 */
class SquareScreenNotInitializedException : IllegalStateException(
    "SquareScreen has not been initialized. " +
    "Call SquareScreen.init(context, config) in Application.onCreate() before using the SDK."
)
