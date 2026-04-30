package io.squarescreen.core.result

/**
 * A discriminated union representing the outcome of a SquareScreen SDK operation.
 *
 * Use Kotlin's `when` expression to handle both cases exhaustively:
 * ```kotlin
 * when (val result = squareScreen.refresh()) {
 *     is SquareScreenResult.Success -> showPlaylist(result.data)
 *     is SquareScreenResult.Error   -> showError(result.error)
 * }
 * ```
 *
 * Convenience extensions: [getOrNull], [errorOrNull], [isSuccess], [isError].
 */
sealed class SquareScreenResult<out T> {
    /** The operation succeeded. [data] holds the result value. */
    data class Success<T>(val data: T) : SquareScreenResult<T>()
    /** The operation failed. [error] describes why. */
    data class Error(val error: SquareScreenError) : SquareScreenResult<Nothing>()
}

/** Returns the [SquareScreenResult.Success.data] value, or null if this is an [SquareScreenResult.Error]. */
fun <T> SquareScreenResult<T>.getOrNull(): T? = (this as? SquareScreenResult.Success)?.data

/** Returns the [SquareScreenError], or null if this is a [SquareScreenResult.Success]. */
fun <T> SquareScreenResult<T>.errorOrNull(): SquareScreenError? = (this as? SquareScreenResult.Error)?.error

/** Returns true if this result is a [SquareScreenResult.Success]. */
val <T> SquareScreenResult<T>.isSuccess: Boolean get() = this is SquareScreenResult.Success

/** Returns true if this result is a [SquareScreenResult.Error]. */
val <T> SquareScreenResult<T>.isError: Boolean get() = this is SquareScreenResult.Error
