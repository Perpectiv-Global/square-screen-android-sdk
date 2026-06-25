package io.squarescreen.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.squarescreen.core.model.PairingRegistration
import io.squarescreen.core.model.PairingStatus
import io.squarescreen.core.result.SquareScreenError
import io.squarescreen.core.result.SquareScreenResult
import io.squarescreen.network.api.PairingApiService
import io.squarescreen.network.interceptor.DemoBypassInterceptor
import io.squarescreen.network.dto.ActivateRequestDto
import io.squarescreen.network.dto.PairingErrorDto
import io.squarescreen.network.dto.PairStatusRequestDto
import io.squarescreen.network.dto.RegisterRequestDto
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Unauthenticated network client for the device pairing flow.
 *
 * Uses its own OkHttpClient with no [io.squarescreen.network.interceptor.DeviceAuthInterceptor]
 * since device credentials do not exist yet at pairing time.
 *
 * Not intended for direct use by integrators — use [io.squarescreen.player.SquareScreenPairing].
 */
class PairingNetworkClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(DemoBypassInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: PairingApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL.trimEnd('/') + "/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PairingApiService::class.java)

    /**
     * Calls `POST /screen/register` with the device's OS identifier.
     *
     * Returns [SquareScreenResult.Success] with a [PairingRegistration] on 200, or
     * [SquareScreenResult.Error] with [SquareScreenError.NetworkError] code 404 (device not
     * pre-registered) or 409 (already paired).
     */
    suspend fun register(osIdentifier: String): SquareScreenResult<PairingRegistration> {
        return try {
            val response = api.register(RegisterRequestDto(osIdentifier))
            when {
                response.isSuccessful -> {
                    val body = response.body()
                        ?: return SquareScreenResult.Error(
                            SquareScreenError.ParseError("Register response body was null")
                        )
                    SquareScreenResult.Success(
                        PairingRegistration(
                            pairingToken = body.pairingToken,
                            expiresIn = body.expiresIn
                        )
                    )
                }
                response.code() == 404 ->
                    SquareScreenResult.Error(SquareScreenError.NetworkError(404, "DEVICE_NOT_FOUND"))
                response.code() == 409 ->
                    SquareScreenResult.Error(SquareScreenError.NetworkError(409, "ALREADY_PAIRED"))
                else ->
                    SquareScreenResult.Error(
                        SquareScreenError.NetworkError(response.code(), response.message())
                    )
            }
        } catch (e: Exception) {
            SquareScreenResult.Error(SquareScreenError.Unknown(e))
        }
    }

    /**
     * Calls `POST /screen/activate` with the developer-supplied device ID and token.
     *
     * Unlike [register], activate resolves immediately — the server returns the permanent
     * device token on 200 with no pair-status polling required.
     *
     * Returns [PairingStatus.Approved] on success, or the appropriate terminal
     * [PairingStatus] on failure:
     * - [PairingStatus.InvalidToken] — device ID is invalid or expired (401)
     * - [PairingStatus.AlreadyPaired] — device is already paired (409 ALREADY_PAIRED)
     * - [PairingStatus.IdentifierMismatch] — device ID is bound to a different token (409 IDENTIFIER_MISMATCH)
     */
    suspend fun activate(deviceId: String, deviceToken: String): PairingStatus {
        return try {
            val response = api.activate(ActivateRequestDto(deviceId, deviceToken))
            when {
                response.isSuccessful -> {
                    val body = response.body()
                        ?: return PairingStatus.Error(
                            IllegalStateException("Activate response body was null")
                        )
                    PairingStatus.Approved(deviceId, body.deviceToken)
                }
                response.code() == 401 -> PairingStatus.InvalidToken
                response.code() == 409 -> {
                    val errorCode = parseErrorCode(response.errorBody()?.string())
                    when (errorCode) {
                        "ALREADY_PAIRED" -> PairingStatus.AlreadyPaired
                        "IDENTIFIER_MISMATCH" -> PairingStatus.IdentifierMismatch
                        else -> PairingStatus.Error(Exception("409: $errorCode"))
                    }
                }
                else -> PairingStatus.Error(
                    Exception("Unexpected HTTP ${response.code()}: ${response.message()}")
                )
            }
        } catch (e: Exception) {
            PairingStatus.Error(e)
        }
    }

    /**
     * Calls `POST /screen/pair-status` using the pairing token from [register].
     *
     * Returns [PairingStatus.Pending], [PairingStatus.Approved], [PairingStatus.InvalidToken],
     * [PairingStatus.Expired], or [PairingStatus.Error] for unexpected failures.
     */
    suspend fun getPairStatus(pairingToken: String): PairingStatus {
        if (pairingToken.isBlank()) {
            return PairingStatus.Error(IllegalStateException("Pairing token is missing — cannot check pair status"))
        }
        return try {
            val response = api.getPairStatus(PairStatusRequestDto(pairingToken))
            when {
                response.isSuccessful -> {
                    val body = response.body()
                        ?: return PairingStatus.Error(
                            IllegalStateException("Pair-status response body was null")
                        )
                    when (body.status) {
                        "pending" -> PairingStatus.Pending
                        "approved" -> {
                            val id = body.deviceId
                                ?: return PairingStatus.Error(
                                    IllegalStateException("Approved response missing device_id")
                                )
                            val token = body.deviceToken
                                ?: return PairingStatus.Error(
                                    IllegalStateException("Approved response missing device_token")
                                )
                            PairingStatus.Approved(id, token)
                        }
                        else -> PairingStatus.Error(
                            IllegalStateException("Unknown pair-status value: '${body.status}'")
                        )
                    }
                }
                response.code() == 401 -> PairingStatus.InvalidToken
                response.code() == 410 -> PairingStatus.Expired
                else -> PairingStatus.Error(
                    Exception("Unexpected HTTP ${response.code()}: ${response.message()}")
                )
            }
        } catch (e: Exception) {
            PairingStatus.Error(e)
        }
    }

    private fun parseErrorCode(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            json.decodeFromString<PairingErrorDto>(errorBody).code
        } catch (e: Exception) {
            null
        }
    }
}
