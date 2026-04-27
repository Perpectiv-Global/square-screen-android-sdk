package io.squarescreen.core.logging

import android.util.Log

/**
 * A built-in logger that writes to Android Logcat. Intended for development only.
 *
 * Usage:
 * ```kotlin
 * logger = if (BuildConfig.DEBUG) SquareScreenDebugLogger() else null
 * ```
 *
 * Never ship this in a production build.
 */
class SquareScreenDebugLogger : SquareScreenLogger {
    override fun debug(tag: String, message: String) { Log.d(tag, message) }
    override fun info(tag: String, message: String) { Log.i(tag, message) }
    override fun warn(tag: String, message: String) { Log.w(tag, message) }
    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
