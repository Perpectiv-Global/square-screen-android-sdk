package io.squarescreen.network.api

import io.squarescreen.network.dto.PairStatusRequestDto
import io.squarescreen.network.dto.PairStatusResponseDto
import io.squarescreen.network.dto.RegisterRequestDto
import io.squarescreen.network.dto.RegisterResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface PairingApiService {

    @POST("screen/register")
    suspend fun register(
        @Body body: RegisterRequestDto
    ): Response<RegisterResponseDto>

    @POST("screen/pair-status")
    suspend fun getPairStatus(
        @Body body: PairStatusRequestDto
    ): Response<PairStatusResponseDto>
}
