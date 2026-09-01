package io.squarescreen.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterRequestDto(
    @SerialName("os_identifier") val osIdentifier: String
)

@Serializable
internal data class ActivateRequestDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_token") val deviceToken: String
)

@Serializable
internal data class ActivateResponseDto(
    val status: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_token") val deviceToken: String,
    val message: String? = null
)

@Serializable
internal data class RegisterResponseDto(
    @SerialName("pairing_token") val pairingToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    val message: String? = null
)

@Serializable
internal data class PairStatusRequestDto(
    @SerialName("pairing_token") val pairingToken: String
)

@Serializable
internal data class PairStatusResponseDto(
    val status: String,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("device_token") val deviceToken: String? = null,
    val message: String? = null
)

@Serializable
internal data class PairingErrorDto(
    val code: String,
    val message: String
)
