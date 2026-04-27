package io.squarescreen.core.logging

/**
 * Pluggable logging interface. Pass an implementation to
 * [io.squarescreen.core.config.SquareScreenConfig.logger] at init time.
 *
 * The SDK never calls [android.util.Log] directly — all internal logging
 * goes through this interface.
 *
 * Pass `null` (the default) for completely silent operation in all build types.
 */
interface SquareScreenLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
