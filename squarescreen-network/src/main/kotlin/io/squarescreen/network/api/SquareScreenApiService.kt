package io.squarescreen.network.api

import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.network.dto.EmergencyResponseDto
import io.squarescreen.network.dto.HeartbeatResponseDto
import io.squarescreen.network.dto.NowPlayingResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
}
