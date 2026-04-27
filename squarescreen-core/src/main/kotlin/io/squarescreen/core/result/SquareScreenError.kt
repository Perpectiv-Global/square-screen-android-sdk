package io.squarescreen.core.result

sealed class SquareScreenError {

    /** A network request failed. [code] is the HTTP status code, or -1 for connectivity errors. */
    data class NetworkError(
        val code: Int,
        val message: String
    ) : SquareScreenError()

    /** The device token was rejected or has expired. */
    data class AuthError(
        val message: String
    ) : SquareScreenError()

    /** Reading from or writing to the local cache failed. */
    data class CacheError(
        val message: String
    ) : SquareScreenError()

    /** The server response could not be parsed into the expected model. */
    data class ParseError(
        val message: String
    ) : SquareScreenError()

    /**
     * An active emergency broadcast is overriding normal playback.
     * Normal playlist operations will not proceed until the alert is cleared.
     */
    data object EmergencyOverrideActive : SquareScreenError()

    /** An unexpected error occurred. Inspect [throwable] for details. */
    data class Unknown(
        val throwable: Throwable
    ) : SquareScreenError()
}
