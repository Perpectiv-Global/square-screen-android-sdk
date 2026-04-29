package io.squarescreen.network.api

import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.core.model.PlaybackReport
import io.squarescreen.network.dto.AckRequestDto
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

internal interface SquareScreenApiService {

    @GET("api/v1/screen/now-playing")
    suspend fun getNowPlaying(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("quality") quality: String? = "1080p",
        @Query("limit") limit: Int? = 20
    ): Response<NowPlayingResponseDto>

    @POST("api/v1/screen/heartbeat")
    suspend fun postHeartbeat(
        @Body payload: HeartbeatPayload
    ): Response<HeartbeatResponseDto>

    @GET("api/v1/screen/emergency")
    suspend fun getEmergencyStatus(): Response<EmergencyResponseDto>

    @POST("api/v1/screen/playback")
    suspend fun reportPlayback(
        @Body report: PlaybackReport
    ): Response<PlaybackReportResponseDto>

    @GET("api/v1/screen/commands")
    suspend fun getCommands(): Response<CommandsResponseDto>

    @POST("api/v1/screen/commands/{commandId}/ack")
    suspend fun acknowledgeCommand(
        @Path("commandId") commandId: String,
        @Body body: AckRequestDto
    ): Response<AckResponseDto>
}
