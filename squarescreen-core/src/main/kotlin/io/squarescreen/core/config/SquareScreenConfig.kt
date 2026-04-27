package io.squarescreen.core.config

import io.squarescreen.core.cache.CacheProvider
import io.squarescreen.core.logging.SquareScreenLogger

/**
 * Immutable configuration passed to [io.squarescreen.player.SquareScreen.init].
 *
 * @param baseUrl Base URL of the SquareScreen API (e.g. "https://api.squarescreen.io").
 * @param deviceId Unique identifier for this device (UUID). Injected as `X-Device-Id`.
 * @param deviceToken Auth token for this device. Injected as `X-Device-Token`.
 *   **Security:** Do not hardcode this value. Store it in `EncryptedSharedPreferences`
 *   and retrieve it from a secure backend on first launch.
 * @param heartbeatIntervalSeconds How often (in seconds) to post a heartbeat.
 *   Minimum 30, default 60.
 * @param emergencyPollIntervalSeconds How often (in seconds) to poll for emergency alerts.
 *   Minimum 15, default 30.
 * @param cacheTtlSeconds How long cached content is considered fresh before re-fetching.
 *   Default 3600 (1 hour).
 * @param foregroundNotification Configuration for the persistent playback notification.
 * @param cacheProvider Custom cache implementation. Pass null to use the built-in
 *   Room + disk cache from `squarescreen-cache`.
 * @param logger Custom logger implementation. Pass null (default) for silent operation.
 */
data class SquareScreenConfig(
    val baseUrl: String,
    val deviceId: String,
    val deviceToken: String,
    val heartbeatIntervalSeconds: Long = 60L,
    val emergencyPollIntervalSeconds: Long = 30L,
    val cacheTtlSeconds: Long = 3600L,
    val foregroundNotification: ForegroundNotificationConfig,
    val cacheProvider: CacheProvider? = null,
    val logger: SquareScreenLogger? = null
) {
    init {
        require(heartbeatIntervalSeconds >= 30) {
            "heartbeatIntervalSeconds must be at least 30, got $heartbeatIntervalSeconds"
        }
        require(emergencyPollIntervalSeconds >= 15) {
            "emergencyPollIntervalSeconds must be at least 15, got $emergencyPollIntervalSeconds"
        }
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(deviceId.isNotBlank()) { "deviceId must not be blank" }
        require(deviceToken.isNotBlank()) { "deviceToken must not be blank" }
    }
}
