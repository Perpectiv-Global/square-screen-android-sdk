package io.squarescreen.player.internal

import io.squarescreen.core.cache.CacheProvider
import io.squarescreen.core.datasource.NetworkDataSource
import io.squarescreen.core.model.Command
import io.squarescreen.core.model.DeviceStatus
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.core.model.PlaybackReport
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PlayerRepositoryTest {

    private lateinit var fakeNetwork: FakeNetworkDataSource
    private lateinit var fakeCache: FakeCacheProvider
    private lateinit var repository: PlayerRepository

    @Before
    fun setUp() {
        fakeNetwork = FakeNetworkDataSource()
        fakeCache = FakeCacheProvider()
        repository = PlayerRepository(fakeNetwork, fakeCache)
        // Reset device status state between tests
        SquareScreenServiceLocator.deviceStatusState.value = DeviceStatus.CONNECTING
    }

    @Test
    fun `fetchPlaylist always hits network first`() = runTest {
        val cachedPlaylist = emptyPlaylist(cachedAt = System.currentTimeMillis())
        fakeCache.storedPlaylist = cachedPlaylist
        val networkPlaylist = emptyPlaylist(cachedAt = System.currentTimeMillis())
        fakeNetwork.nowPlayingResult = SquareScreenResult.Success(networkPlaylist)

        val result = repository.fetchPlaylist()

        assertTrue(result is SquareScreenResult.Success)
        assertEquals(networkPlaylist, (result as SquareScreenResult.Success).data)
        assertEquals(1, fakeNetwork.fetchNowPlayingCallCount)
        assertEquals(DeviceStatus.ONLINE, SquareScreenServiceLocator.deviceStatusState.value)
    }

    @Test
    fun `fetchPlaylist hits network on cache miss`() = runTest {
        fakeCache.storedPlaylist = null
        val networkPlaylist = emptyPlaylist(cachedAt = System.currentTimeMillis())
        fakeNetwork.nowPlayingResult = SquareScreenResult.Success(networkPlaylist)

        val result = repository.fetchPlaylist()

        assertTrue(result is SquareScreenResult.Success)
        assertEquals(1, fakeNetwork.fetchNowPlayingCallCount)
        assertEquals(DeviceStatus.ONLINE, SquareScreenServiceLocator.deviceStatusState.value)
    }

    @Test
    fun `fetchPlaylist saves to cache after network success`() = runTest {
        fakeCache.storedPlaylist = null
        val networkPlaylist = emptyPlaylist(cachedAt = System.currentTimeMillis())
        fakeNetwork.nowPlayingResult = SquareScreenResult.Success(networkPlaylist)

        repository.fetchPlaylist()

        assertEquals(networkPlaylist, fakeCache.storedPlaylist)
    }

    @Test
    fun `fetchPlaylist returns stale in-memory fallback on network failure`() = runTest {
        // Seed the last known playlist via a successful fetch
        fakeCache.storedPlaylist = null
        val playlist = emptyPlaylist(cachedAt = System.currentTimeMillis())
        fakeNetwork.nowPlayingResult = SquareScreenResult.Success(playlist)
        repository.fetchPlaylist() // warms up lastKnownPlaylist

        // Now make cache miss + network fail
        fakeCache.storedPlaylist = null
        fakeNetwork.nowPlayingResult = SquareScreenResult.Error(
            SquareScreenError.NetworkError(503, "Service Unavailable")
        )

        val result = repository.fetchPlaylist()

        assertTrue(result is SquareScreenResult.Success)
        assertEquals(playlist, (result as SquareScreenResult.Success).data)
        assertEquals(DeviceStatus.OFFLINE, SquareScreenServiceLocator.deviceStatusState.value)
    }

    @Test
    fun `fetchPlaylist propagates error when no fallback available`() = runTest {
        fakeCache.storedPlaylist = null
        val error = SquareScreenError.NetworkError(503, "Service Unavailable")
        fakeNetwork.nowPlayingResult = SquareScreenResult.Error(error)

        val result = repository.fetchPlaylist()

        assertTrue(result is SquareScreenResult.Error)
        assertEquals(error, (result as SquareScreenResult.Error).error)
        assertEquals(DeviceStatus.OFFLINE, SquareScreenServiceLocator.deviceStatusState.value)
    }

    // --- Fakes ---

    private fun emptyPlaylist(cachedAt: Long) = Playlist(
        items = emptyList(),
        strategy = null,
        schedule = null,
        playlist = null,
        cachedAt = cachedAt
    )

    private class FakeNetworkDataSource : NetworkDataSource {
        var nowPlayingResult: SquareScreenResult<Playlist> =
            SquareScreenResult.Success(Playlist(emptyList(), null, null, null, 0L))
        var fetchNowPlayingCallCount = 0

        override suspend fun fetchNowPlaying(
            type: String?, category: String?, quality: String?, limit: Int?
        ): SquareScreenResult<Playlist> {
            fetchNowPlayingCallCount++
            return nowPlayingResult
        }

        override suspend fun sendHeartbeat(payload: HeartbeatPayload): SquareScreenResult<Unit> =
            SquareScreenResult.Success(Unit)

        override suspend fun fetchEmergencyAlert(): SquareScreenResult<EmergencyAlert?> =
            SquareScreenResult.Success(null)

        override suspend fun reportPlayback(reports: List<PlaybackReport>): SquareScreenResult<Unit> =
            SquareScreenResult.Success(Unit)

        override suspend fun fetchCommands(): SquareScreenResult<List<Command>> =
            SquareScreenResult.Success(emptyList())

        override suspend fun acknowledgeCommand(
            commandId: String, status: String, result: Map<String, String>
        ): SquareScreenResult<Unit> = SquareScreenResult.Success(Unit)
    }

    private class FakeCacheProvider : CacheProvider {
        var storedPlaylist: Playlist? = null

        override suspend fun getPlaylist(): Playlist? = storedPlaylist
        override suspend fun savePlaylist(playlist: Playlist) { storedPlaylist = playlist }
        override suspend fun getMediaFile(url: String): File? = null
        override suspend fun saveMediaFile(url: String, file: File) {}
        override suspend fun clearAll() { storedPlaylist = null }
    }
}
