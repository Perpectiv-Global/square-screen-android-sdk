package io.squarescreen.core.model

enum class DeviceStatus {
    /** Establishing initial connection to the SquareScreen API. */
    CONNECTING,
    /** Connected and in sync with the server. */
    ONLINE,
    /** No network connectivity; serving from cache. */
    OFFLINE,
    /** Connected but actively fetching an update. */
    SYNCING
}
