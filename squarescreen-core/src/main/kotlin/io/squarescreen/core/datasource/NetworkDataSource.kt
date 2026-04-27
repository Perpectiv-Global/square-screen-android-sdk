package io.squarescreen.core.datasource

import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.result.SquareScreenResult

/**
 * Contract for the remote data layer. Implemented by `squarescreen-network`;
 * not exposed as a public dependency to integrators.
 */
interface NetworkDataSource {

    /**
     * Fetches the active playlist for the authenticated device.
     *
     * @param type Optional media type filter ("image" or "video").
     * @param category Optional category filter.
     * @param quality Desired media quality (default "1080p").
     * @param limit Maximum number of items to return (default 20).
     */
    suspend fun fetchNowPlaying(
        type: String? = null,
        category: String? = null,
        quality: String? = "1080p",
        limit: Int? = 20
    ): SquareScreenResult<Playlist>

    /**
     * Posts a device health heartbeat to the server.
     */
    suspend fun sendHeartbeat(payload: HeartbeatPayload): SquareScreenResult<Unit>

    /**
     * Checks whether there is an active emergency broadcast for this device.
     * Returns null inside the result when no alert is active.
     */
    suspend fun fetchEmergencyAlert(): SquareScreenResult<EmergencyAlert?>
}
