package io.squarescreen.network

import io.squarescreen.core.datasource.NetworkDataSource
import io.squarescreen.core.model.EmergencyAlert
import io.squarescreen.core.model.HeartbeatPayload
import io.squarescreen.core.model.Playlist
import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.network.api.SquareScreenApiService
import io.squarescreen.network.mapper.NetworkMapper

internal class NetworkDataSourceImpl(
    private val api: SquareScreenApiService
) : NetworkDataSource {

    override suspend fun fetchNowPlaying(
        type: String?,
        category: String?,
        quality: String?,
        limit: Int?
    ): SquareScreenResult<Playlist> = safeApiCall {
        val response = api.getNowPlaying(type, category, quality, limit)
        if (response.isSuccessful) {
            val body = response.body() ?: return@safeApiCall SquareScreenResult.Error(
                SquareScreenError.ParseError("Now-playing response body was null")
            )
            SquareScreenResult.Success(
                NetworkMapper.mapNowPlaying(body, cachedAt = System.currentTimeMillis())
            )
        } else {
            SquareScreenResult.Error(mapHttpError(response.code(), response.message()))
        }
    }

    override suspend fun sendHeartbeat(payload: HeartbeatPayload): SquareScreenResult<Unit> =
        safeApiCall {
            val response = api.postHeartbeat(payload)
            if (response.isSuccessful) {
                SquareScreenResult.Success(Unit)
            } else {
                SquareScreenResult.Error(mapHttpError(response.code(), response.message()))
            }
        }

    override suspend fun fetchEmergencyAlert(): SquareScreenResult<EmergencyAlert?> =
        safeApiCall {
            val response = api.getEmergencyStatus()
            if (response.isSuccessful) {
                val body = response.body() ?: return@safeApiCall SquareScreenResult.Error(
                    SquareScreenError.ParseError("Emergency response body was null")
                )
                val alert = if (body.active && body.broadcast != null) {
                    NetworkMapper.mapEmergencyBroadcast(body.broadcast)
                } else {
                    null
                }
                SquareScreenResult.Success(alert)
            } else {
                SquareScreenResult.Error(mapHttpError(response.code(), response.message()))
            }
        }

    private fun mapHttpError(code: Int, message: String): SquareScreenError {
        return when (code) {
            401, 403 -> SquareScreenError.AuthError("Device authentication failed (HTTP $code)")
            else -> SquareScreenError.NetworkError(code, message)
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> SquareScreenResult<T>): SquareScreenResult<T> {
        return try {
            block()
        } catch (e: Exception) {
            SquareScreenResult.Error(SquareScreenError.Unknown(e))
        }
    }
}
