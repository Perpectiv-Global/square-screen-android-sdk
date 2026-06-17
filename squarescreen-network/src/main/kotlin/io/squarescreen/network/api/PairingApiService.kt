package io.squarescreen.network.api

import io.squarescreen.network.dto.PairStatusResponseDto
import io.squarescreen.network.dto.RegisterRequestDto
import io.squarescreen.network.dto.RegisterResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

internal interface PairingApiService {

    @POST("screen/register")
    suspend fun register(
        @Body body: RegisterRequestDto
    ): Response<RegisterResponseDto>

    @GET("screen/pair-status")
    suspend fun getPairStatus(
        @Header("Authorization") authHeader: String
    ): Response<PairStatusResponseDto>
}
