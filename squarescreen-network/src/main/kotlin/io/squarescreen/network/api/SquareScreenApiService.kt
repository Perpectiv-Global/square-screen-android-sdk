package io.squarescreen.network.api

import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.network.dto.AckRequestDto
import io.squarescreen.network.dto.PlaybackReportRequestDto
import io.squarescreen.network.dto.AckResponseDto
import io.squarescreen.network.dto.CommandsResponseDto
import io.squarescreen.network.dto.EmergencyResponseDto
import io.squarescreen.network.dto.HeartbeatResponseDto
import io.squarescreen.network.dto.NowPlayingResponseDto
import io.squarescreen.network.dto.PlaybackReportResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Paths are relative to the base URL which already includes /api/v1/
internal interface SquareScreenApiService {

    @GET("screen/now-playing")
    suspend fun getNowPlaying(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("quality") quality: String? = "1080p",
        @Query("limit") limit: Int? = 20
    ): Response<NowPlayingResponseDto>

    @POST("screen/heartbeat")
    suspend fun postHeartbeat(
        @Body payload: HeartbeatPayload
    ): Response<HeartbeatResponseDto>

    @GET("screen/emergency")
    suspend fun getEmergencyStatus(): Response<EmergencyResponseDto>

    @POST("screen/playback")
    suspend fun reportPlayback(
        @Body body: PlaybackReportRequestDto
    ): Response<PlaybackReportResponseDto>

    @GET("screen/commands")
    suspend fun getCommands(): Response<CommandsResponseDto>

    @POST("screen/commands/{commandId}/ack")
    suspend fun acknowledgeCommand(
        @Path("commandId") commandId: String,
        @Body body: AckRequestDto
    ): Response<AckResponseDto>
}
