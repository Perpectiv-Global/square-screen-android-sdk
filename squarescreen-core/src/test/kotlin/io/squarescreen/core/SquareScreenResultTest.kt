package io.squarescreen.core

import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.core.result.errorOrNull
import io.squarescreen.core.result.getOrNull
import io.squarescreen.core.result.isError
import io.squarescreen.core.result.isSuccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SquareScreenResultTest {

    @Test
    fun `success result returns data`() {
        val result = SquareScreenResult.Success("hello")
        assertEquals("hello", result.getOrNull())
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertNull(result.errorOrNull())
    }

    @Test
    fun `error result returns error`() {
        val error = SquareScreenError.NetworkError(404, "Not found")
        val result = SquareScreenResult.Error(error)
        assertNull(result.getOrNull())
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun `sealed error types are distinct`() {
        val network = SquareScreenError.NetworkError(500, "Server error")
        val auth = SquareScreenError.AuthError("Invalid token")
        val cache = SquareScreenError.CacheError("Disk full")
        val parse = SquareScreenError.ParseError("Unexpected JSON")
        val emergency = SquareScreenError.EmergencyOverrideActive
        val unknown = SquareScreenError.Unknown(RuntimeException("boom"))

        assertTrue(network is SquareScreenError.NetworkError)
        assertTrue(auth is SquareScreenError.AuthError)
        assertTrue(cache is SquareScreenError.CacheError)
        assertTrue(parse is SquareScreenError.ParseError)
        assertTrue(emergency is SquareScreenError.EmergencyOverrideActive)
        assertTrue(unknown is SquareScreenError.Unknown)
    }

    @Test
    fun `SquareScreenConfig rejects short heartbeat interval`() {
        try {
            io.squarescreen.core.config.SquareScreenConfig(
                baseUrl = "https://api.squarescreen.io",
                deviceId = "id",
                deviceToken = "token",
                heartbeatIntervalSeconds = 10L,
                foregroundNotification = io.squarescreen.core.config.ForegroundNotificationConfig(
                    title = "Test",
                    iconResId = 0
                )
            )
            assertTrue("Expected IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("heartbeatIntervalSeconds"))
        }
    }
}
