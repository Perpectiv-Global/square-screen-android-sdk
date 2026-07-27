package io.squarescreen.player.internal

import io.squarescreen.core.cache.CacheProvider
import io.squarescreen.core.datasource.NetworkDataSource
import io.squarescreen.core.model.DeviceStatus
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.PlaybackReport
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.result.SquareScreenResult

private const val TAG = "PlayerRepository"

internal class PlayerRepository(
    private val network: NetworkDataSource,
    private val cache: CacheProvider
) {
    /** Last successfully fetched playlist — serves as stale fallback on network failure. */
    @Volatile private var lastKnownPlaylist: Playlist? = null

    /**
     * Fetches the active playlist using a cache-first strategy:
     * 1. Return cached playlist if fresh (TTL managed by CacheProvider)
     * 2. Fetch from network and cache the result
     * 3. On network failure, return last known playlist (stale fallback) and emit OFFLINE
     * 4. If no fallback exists, propagate the network error
     */
    suspend fun fetchPlaylist(
        type: String? = null,
        category: String? = null,
        quality: String? = null,
        limit: Int? = null
    ): SquareScreenResult<Playlist> {
        SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.SYNCING
        val result = network.fetchNowPlaying(type, category, quality, limit)

        return when (result) {
            is SquareScreenResult.Success -> {
                cache.savePlaylist(result.data)
                lastKnownPlaylist = result.data
                SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.ONLINE
                val p = result.data
                SquareScreenServiceLocator.log(TAG, "Playlist fetched from network — items=${p.items.size} playlistUuid=${p.playlist?.uuid} playlistName=${p.playlist?.name} scheduleUuid=${p.schedule?.uuid} scheduleName=${p.schedule?.name}")
                result
            }
            is SquareScreenResult.Error -> {
                SquareScreenServiceLocator.logError(TAG, "Network fetch failed: ${result.error}")
                val fallback = lastKnownPlaylist ?: cache.getPlaylist()
                if (fallback != null) {
                    lastKnownPlaylist = fallback
                    SquareScreenServiceLocator.log(TAG, "Serving cached playlist as offline fallback (${fallback.items.size} items)")
                    SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.OFFLINE
                    SquareScreenResult.Success(fallback)
                } else {
                    SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.OFFLINE
                    result
                }
            }
        }
    }

    suspend fun fetchEmergencyAlert(): SquareScreenResult<EmergencyAlert?> {
        return network.fetchEmergencyAlert()
    }

    suspend fun reportPlayback(reports: List<PlaybackReport>): SquareScreenResult<Unit> {
        val result = network.reportPlayback(reports)
        when (result) {
            is SquareScreenResult.Success ->
                SquareScreenServiceLocator.log(TAG, "Playback reported: ${reports.size} item(s) → 200 OK")
            is SquareScreenResult.Error ->
                SquareScreenServiceLocator.logError(TAG, "Playback report failed: ${reports.size} item(s) → ${result.error}")
        }
        return result
    }

    suspend fun acknowledgeCommand(
        commandId: String,
        status: String,
        result: Map<String, String>
    ): SquareScreenResult<Unit> {
        return network.acknowledgeCommand(commandId, status, result)
    }
}
