package io.squarescreen.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.TransitionType
import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.network.api.SquareScreenApiService
import io.squarescreen.network.interceptor.DeviceAuthInterceptor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class NetworkDataSourceImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: NetworkDataSourceImpl

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(DeviceAuthInterceptor("test-device-id", "test-device-token"))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(SquareScreenApiService::class.java)
        dataSource = NetworkDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- Auth header injection ---

    @Test
    fun `auth headers are injected on now-playing request`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"items":[],"strategy":null}"""))

        dataSource.fetchNowPlaying()

        val request = mockWebServer.takeRequest()
        assertEquals("test-device-id", request.getHeader("X-Device-Id"))
        assertEquals("test-device-token", request.getHeader("X-Device-Token"))
        assertEquals("application/json", request.getHeader("Accept"))
    }

    @Test
    fun `auth headers are injected on heartbeat request`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success":true}"""))

        dataSource.sendHeartbeat(
            io.squarescreen.core.model.HeartbeatPayload(
                cpuUsage = null, memoryUsage = null, diskUsage = null,
                temperature = null, osVersion = "Android 14", playerVersion = "0.1.0"
            )
        )

        val request = mockWebServer.takeRequest()
        assertEquals("test-device-id", request.getHeader("X-Device-Id"))
        assertEquals("test-device-token", request.getHeader("X-Device-Token"))
    }

    @Test
    fun `auth headers are injected on emergency request`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"active":false,"broadcast":null}"""))

        dataSource.fetchEmergencyAlert()

        val request = mockWebServer.takeRequest()
        assertEquals("test-device-id", request.getHeader("X-Device-Id"))
        assertEquals("test-device-token", request.getHeader("X-Device-Token"))
    }

    // --- Now Playing ---

    @Test
    fun `fetchNowPlaying returns playlist on success`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "items": [
                    {
                      "id": 1,
                      "type": "image",
                      "url": "https://cdn.example.com/banner.jpg",
                      "duration": 10,
                      "transition": "fade"
                    }
                  ],
                  "strategy": {
                    "loop": true,
                    "shuffle": false,
                    "preloadCount": 3,
                    "showThumbnail": true,
                    "defaultTransition": "fade"
                  }
                }
            """.trimIndent()))

        val result = dataSource.fetchNowPlaying()

        assertTrue(result is SquareScreenResult.Success)
        val playlist = (result as SquareScreenResult.Success).data
        assertEquals(1, playlist.items.size)
        val item = playlist.items[0]
        assertEquals(1, item.id)
        assertEquals(MediaType.IMAGE, item.type)
        assertEquals("https://cdn.example.com/banner.jpg", item.url)
        assertEquals(10, item.duration)
        assertEquals(TransitionType.FADE, item.transition)
    }

    @Test
    fun `fetchNowPlaying returns empty playlist when nothing scheduled`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"items":[],"strategy":null}"""))

        val result = dataSource.fetchNowPlaying()

        assertTrue(result is SquareScreenResult.Success)
        val playlist = (result as SquareScreenResult.Success).data
        assertTrue(playlist.items.isEmpty())
    }

    @Test
    fun `fetchNowPlaying returns NetworkError on HTTP 500`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = dataSource.fetchNowPlaying()

        assertTrue(result is SquareScreenResult.Error)
        val error = (result as SquareScreenResult.Error).error
        assertTrue(error is SquareScreenError.NetworkError)
        assertEquals(500, (error as SquareScreenError.NetworkError).code)
    }

    @Test
    fun `fetchNowPlaying returns AuthError on HTTP 401`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val result = dataSource.fetchNowPlaying()

        assertTrue(result is SquareScreenResult.Error)
        assertTrue((result as SquareScreenResult.Error).error is SquareScreenError.AuthError)
    }

    @Test
    fun `fetchNowPlaying returns AuthError on HTTP 403`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(403))

        val result = dataSource.fetchNowPlaying()

        assertTrue(result is SquareScreenResult.Error)
        assertTrue((result as SquareScreenResult.Error).error is SquareScreenError.AuthError)
    }

    @Test
    fun `fetchNowPlaying returns Unknown error on malformed JSON`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("not valid json {{{{"))

        val result = dataSource.fetchNowPlaying()

        assertTrue(result is SquareScreenResult.Error)
        assertTrue((result as SquareScreenResult.Error).error is SquareScreenError.Unknown)
    }

    // --- Heartbeat ---

    @Test
    fun `sendHeartbeat returns Success on 200`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success":true}"""))

        val result = dataSource.sendHeartbeat(
            io.squarescreen.core.model.HeartbeatPayload(
                cpuUsage = 42.5f,
                memoryUsage = 61.0f,
                diskUsage = 28.3f,
                temperature = 52.0f,
                osVersion = "Android 14",
                playerVersion = "0.1.0"
            )
        )

        assertTrue(result is SquareScreenResult.Success)
    }

    @Test
    fun `sendHeartbeat sends all fields in request body`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success":true}"""))

        dataSource.sendHeartbeat(
            io.squarescreen.core.model.HeartbeatPayload(
                cpuUsage = 42.5f,
                memoryUsage = 61.0f,
                diskUsage = 28.3f,
                temperature = 52.0f,
                osVersion = "Android 13",
                playerVersion = "1.2.0"
            )
        )

        val body = mockWebServer.takeRequest().body.readUtf8()
        assertTrue(body.contains("cpu_usage"))
        assertTrue(body.contains("memory_usage"))
        assertTrue(body.contains("disk_usage"))
        assertTrue(body.contains("temperature"))
        assertTrue(body.contains("os_version"))
        assertTrue(body.contains("player_version"))
    }

    @Test
    fun `sendHeartbeat handles null temperature gracefully`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success":true}"""))

        val result = dataSource.sendHeartbeat(
            io.squarescreen.core.model.HeartbeatPayload(
                cpuUsage = null,
                memoryUsage = null,
                diskUsage = null,
                temperature = null,
                osVersion = "Android 14",
                playerVersion = "0.1.0"
            )
        )

        assertTrue(result is SquareScreenResult.Success)
    }

    // --- Emergency ---

    @Test
    fun `fetchEmergencyAlert returns alert when active`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "active": true,
                  "broadcast": {
                    "id": "uuid-123",
                    "title": "FIRE ALARM",
                    "message": "Evacuate immediately.",
                    "background_color": "#FF0000",
                    "text_color": "#FFFFFF"
                  }
                }
            """.trimIndent()))

        val result = dataSource.fetchEmergencyAlert()

        assertTrue(result is SquareScreenResult.Success)
        val alert = (result as SquareScreenResult.Success).data
        assertNotNull(alert)
        assertEquals("uuid-123", alert!!.id)
        assertEquals("FIRE ALARM", alert.title)
        assertEquals("Evacuate immediately.", alert.message)
        assertEquals("#FF0000", alert.backgroundColor)
        assertEquals("#FFFFFF", alert.textColor)
    }

    @Test
    fun `fetchEmergencyAlert returns null when not active`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"active":false,"broadcast":null}"""))

        val result = dataSource.fetchEmergencyAlert()

        assertTrue(result is SquareScreenResult.Success)
        assertNull((result as SquareScreenResult.Success).data)
    }

    @Test
    fun `fetchEmergencyAlert returns NetworkError on HTTP 500`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = dataSource.fetchEmergencyAlert()

        assertTrue(result is SquareScreenResult.Error)
        assertTrue((result as SquareScreenResult.Error).error is SquareScreenError.NetworkError)
    }
}
