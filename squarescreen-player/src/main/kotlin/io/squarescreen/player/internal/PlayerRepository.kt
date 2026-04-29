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
        val cached = cache.getPlaylist()
        if (cached != null) {
            SquareScreenServiceLocator.log(TAG, "Serving playlist from cache (${cached.items.size} items)")
            lastKnownPlaylist = cached
            SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.ONLINE
            return SquareScreenResult.Success(cached)
        }

        SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.SYNCING
        val result = network.fetchNowPlaying(type, category, quality, limit)

        return when (result) {
            is SquareScreenResult.Success -> {
                cache.savePlaylist(result.data)
                lastKnownPlaylist = result.data
                SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.ONLINE
                SquareScreenServiceLocator.log(TAG, "Playlist fetched from network (${result.data.items.size} items)")
                result
            }
            is SquareScreenResult.Error -> {
                SquareScreenServiceLocator.logError(TAG, "Network fetch failed: ${result.error}")
                val fallback = lastKnownPlaylist
                if (fallback != null) {
                    SquareScreenServiceLocator.log(TAG, "Serving stale in-memory playlist as offline fallback")
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

    suspend fun reportPlayback(report: PlaybackReport): SquareScreenResult<Unit> {
        return network.reportPlayback(report)
    }

    suspend fun acknowledgeCommand(
        commandId: String,
        status: String,
        result: Map<String, String>
    ): SquareScreenResult<Unit> {
        return network.acknowledgeCommand(commandId, status, result)
    }
}
